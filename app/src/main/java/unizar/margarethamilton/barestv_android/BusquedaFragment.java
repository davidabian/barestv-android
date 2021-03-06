package unizar.margarethamilton.barestv_android;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import unizar.margarethamilton.connection.ClienteRest;
import unizar.margarethamilton.dataBase.FavoritosDbAdapter;
import unizar.margarethamilton.listViewConfig.ListHashAdapter;

public class BusquedaFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "clienteRest";

    private SearchView searchView;
    private TextView listText;
    private TextView noResText;
    private ListView mList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<HashMap<String, String>> pGlobal = null;
    private ArrayAdapter adapterGlobal = null;

    private Snackbar snackbar = null;
    private Snackbar snackbarFlitro = null;

    private FavoritosDbAdapter mDbHelper; // Acceso a BBDD

    private boolean hayBusqueda = false;
    private boolean hayFiltro = false;
    private boolean hayFiltroFecha = false;
    private String ultimaBusqueda = "";
    private String filtroCategoria="";

    private int filtroFechaDia;
    private int filtroFechaMes;
    private int filtroFechaAño;

    private static ClienteRest clienteRest;
    private OnFragmentBusquedaInteractionListener mListener;

    public BusquedaFragment(){}

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param _clienteRest _clienteRest API para conectar a la BBDD remota.
     * @return A new instance of fragment BusquedaFragment.
     */
    public static BusquedaFragment newInstance(ClienteRest _clienteRest) {
        BusquedaFragment fragment = new BusquedaFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, _clienteRest);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            clienteRest = (ClienteRest) getArguments().getSerializable(ARG_PARAM1);
        }
        setRetainInstance(true);
        mDbHelper = new FavoritosDbAdapter(this.getActivity());
        mDbHelper.open();
        setUserVisibleHint(false);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.busqueda_fragment_layout, container, false);
        mList = (ListView) view.findViewById(R.id.resList);
        noResText=(TextView) view.findViewById(R.id.empty_list_item);
        searchView = (SearchView) view.findViewById(R.id.sview);
        searchView.setIconifiedByDefault(false);
        Toolbar mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.menu_toolbar);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                abrirFiltro();
                return false;
            }
        });
        searchView.setQueryHint(getString(R.string.search_hint));
        listText = (TextView) view.findViewById(R.id.listText);
        listText.setText(R.string.progProx);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView bar = (TextView) parent.findViewById(R.id.bar);
                mListener.programaPulsado((String)bar.getText());
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefreshProx);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        new SetProgProxTask().execute();
                        //swipeRefreshLayout.setRefreshing(false);
                    }
                }
        );

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(query.contains("Bar:")){
                    String realQuery = query.split("Bar:")[1];
                    listText.setText(getString(R.string.TextoBusqueda)+realQuery);
                }else {
                    listText.setText(getString(R.string.TextoBusqueda) + query);
                }
                new SetBusquedaTask(query).execute();
                swipeRefreshLayout.setEnabled(false);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText.isEmpty()){
                    new SetProgProxTask().execute();
                    listText.setText(R.string.progProx);
                }
//                listText.setText(newText);
                return false;
            }
        });

        ImageView closeButton = (ImageView)searchView.findViewById(R.id.search_close_btn);
        // Set on click listener
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listText.setText(R.string.progProx);
                searchView.setQuery("",false);
                new SetProgProxTask().execute();
                swipeRefreshLayout.setEnabled(true);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {

                return false;
            }
        });


        //Se obtiene la programacion proxima
        new SetProgProxTask().execute();

        if(snackbarFlitro!=null){
            snackbarFlitro.show();
        }
        return view;
    }

    public void busquedaClick (View v) {
        RelativeLayout vwParentRow = (RelativeLayout)v.getParent();
        CheckBox favoritoV = (CheckBox) vwParentRow.findViewById(R.id.favCheck);
        if (favoritoV.isChecked()) {

            TextView tituloV = (TextView) vwParentRow.findViewById(R.id.titulo);
            TextView barV = (TextView) vwParentRow.findViewById(R.id.bar);
            TextView descrV = (TextView) vwParentRow.findViewById(R.id.descr);
            TextView catV = (TextView) vwParentRow.findViewById(R.id.categoria);
            TextView inicioV = (TextView) vwParentRow.findViewById(R.id.inicio);
            TextView finV = (TextView) vwParentRow.findViewById(R.id.fin);

            try {
                mDbHelper.introducirFavoritos(tituloV.getText().toString(), barV.getText().toString(),
                        descrV.getText().toString(), inicioV.getText().toString(),
                        finV.getText().toString(), catV.getText().toString());
                pGlobal.get(mList.getPositionForView(vwParentRow)).put("CheckBox", "1") ;
                adapterGlobal.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            TextView tituloV = (TextView) vwParentRow.findViewById(R.id.titulo);
            TextView barV = (TextView) vwParentRow.findViewById(R.id.bar);
            mDbHelper.EliminarFavoritos(tituloV.getText().toString(), barV.getText().toString());
            try {
                pGlobal.get(mList.getPositionForView(vwParentRow)).put("CheckBox", "0");
                adapterGlobal.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.isVisible()) {
            if (isVisibleToUser) {

                if (hayFiltro) {
                    snackbarFlitro.show();
                }

                mList.setAdapter(null);
                if (hayBusqueda)
                {
                    new SetBusquedaTask(ultimaBusqueda).execute();
                } else {
                    swipeRefreshLayout.setRefreshing(true);
                    new SetProgProxTask().execute();
                }
            } else {
                if (hayFiltro) {
                    snackbarFlitro.dismiss();
                }
            }
        }
    }

    private void abrirFiltro() {
        Intent i = new Intent(getActivity(), FiltrosActivity.class);
        i.putExtra("ClitenteRest",clienteRest);
        startActivityForResult(i, 1);
    }

    public void programacionBar(String bar){
        listText.setText(getString(R.string.TextoBusqueda)+bar);
        new SetBusquedaTask("Bar:"+bar).execute();
    }

    public void quitarFiltros(){
         snackbarFlitro = Snackbar
                .make(mList, "Filtros Activos", Snackbar.LENGTH_LONG)
                .setAction("Quitar Filtros", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar snackbar1 = Snackbar.make(getView(), "Filtros eliminados", Snackbar.LENGTH_SHORT);
                        snackbar1.show();
                        hayFiltro=false;
                        hayFiltroFecha=false;
                        filtroCategoria="";
                        if(hayBusqueda){
                            new SetBusquedaTask(ultimaBusqueda).execute();
                        }else {
                            new SetProgProxTask().execute();
                        }
                    }
                });
        snackbarFlitro.setDuration(Snackbar.LENGTH_INDEFINITE);
        snackbarFlitro.show();
    }
/*Aplica el filtros a la programacion resultado*/
    public void aplicarFiltros(String categoria, int dia, int mes, int año){
        if(!categoria.isEmpty()) {
            filtroCategoria = categoria;
        }
        if(dia!=0){
            hayFiltroFecha=true;
            filtroFechaDia=dia;
            filtroFechaMes=mes+1;
            filtroFechaAño=año;
//            if(dia<10){
//                filtroFechaDia="0"+filtroFechaDia;
//            }
//            if(mes<10){
//                filtroFechaMes="0"+filtroFechaMes;
//            }
        }
        mList.setAdapter(null);
        swipeRefreshLayout.setRefreshing(true);
        if(hayBusqueda){
            new SetBusquedaTask(ultimaBusqueda);
        }else {
             new SetProgProxTask().execute();
        }

    }
    /*Interfaz para comunicar a la busqueda el bar pulsado*/
    public interface OnFragmentBusquedaInteractionListener {
        void programaPulsado(String bar);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentBusquedaInteractionListener) {
            mListener = (OnFragmentBusquedaInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
//            if (data.hasExtra("Tab")) {
//                mViewPager.setCurrentItem(3);
//            }

            if(data.hasExtra("FiltroCategoria")){
                hayFiltro=true;
               aplicarFiltros(data.getStringExtra("FiltroCategoria"),data.getIntExtra("FiltroFechaDia",0),
                                data.getIntExtra("FiltroFechaMes",0),
                                data.getIntExtra("FiltroFechaAño",0));
            }else{
                hayFiltro=true;
                aplicarFiltros("",data.getIntExtra("FiltroFechaDia",0),
                        data.getIntExtra("FiltroFechaMes",0),
                        data.getIntExtra("FiltroFechaAño",0));
            }
    }



    /**
     * Rellena el listview con datods dados por el API de forma asincrona para el caso de programacion proxima
     */
    private class SetProgProxTask extends AsyncTask<Void, Void, ArrayAdapter> {

        protected void onPreExecute () {
            swipeRefreshLayout.setRefreshing(true);
            pGlobal = null;
        }

        /**
         * Comunicacion asincrona
         * @param e void
         * @return ArrayAdapter
         */
        protected ArrayAdapter doInBackground(Void... e) {

            // Eliminamos el snackbar anterior si no esta elinimado
            if (snackbar != null) snackbar.dismiss();

            // Obtiene del BBDD remoto las programaciones destacadas
            List<HashMap<String, String>> programacion = clienteRest.getProgramacionProxima();


            // Si no se ha podido establecer la conexion
            if (programacion == null) return null;

            String titulo;
            String bar;
            for (int i = 0; i < programacion.size(); i++) {
                titulo = programacion.get(i).get("Titulo");
                bar = programacion.get(i).get("Bar");
                if (mDbHelper.comprobarFavorito(titulo,bar)) {
                    programacion.get(i).put("CheckBox", "1");
                } else {
                    programacion.get(i).put("CheckBox", "0");
                }
            }
            hayBusqueda=false;
            // Crear un array donde se especifica los datos que se quiere mostrar
            String[] from = new String[] { "Titulo", "Categoria", "Bar", "Descr", "Inicio", "Fin","CheckBox"};

            // Crear un array done sse especifica los campos de ListView que se quiere rellenar
            int[] to = new int[] { R.id.titulo , R.id.categoria, R.id.bar, R.id.descr,
                    R.id.inicio, R.id.fin,R.id.favCheck};

            pGlobal = programacion;
            ListHashAdapter res = new ListHashAdapter(getActivity(), R.layout.busqueda_listview_content,
                    programacion, from, to);
            if(hayFiltro){
                quitarFiltros();
                List<HashMap<String, String>> programacionFiltrada = new ArrayList<HashMap<String, String>>();
                if(!filtroCategoria.isEmpty()) {
                    for (HashMap<String, String> s : programacion) {
                        if (s.containsValue(filtroCategoria)) {
                            programacionFiltrada.add(s);
                        }
                    }
                    pGlobal = programacionFiltrada;
                    // Configurar el adapter
                    res =  new ListHashAdapter(getActivity(), R.layout.busqueda_listview_content,
                            programacionFiltrada, from, to);
                }
            }
            return res;
        }

        /**
         * Una vez obtenido los datos, se rellena el listview
         * @param adapter
         */
        protected void onPostExecute(ArrayAdapter adapter) {
            noResText.setVisibility(View.GONE);
            if (adapter == null) {
                try {
                    // Mensaje error en caso de no poder conectar con la BBDD
                    snackbar = Snackbar.make(getView(), R.string.error_conexion, Snackbar.LENGTH_INDEFINITE)
                            .setAction("Action", null);
                    snackbar.show();
                } catch (Exception x) { x.printStackTrace();}
                swipeRefreshLayout.setRefreshing(false);
            } else {
                adapterGlobal = adapter;
                mList.setAdapter(adapterGlobal);
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    /**
     * Rellena el listview con datods dados por el API de forma asincrona para el caso de programacion proxima
     */
    private class SetBusquedaTask extends AsyncTask<Void, Void, ArrayAdapter> {

        private String query;
        public SetBusquedaTask(String s){
            this.query=s;
            Calendar c =Calendar.getInstance();

        }

        protected void onPreExecute () {
            swipeRefreshLayout.setRefreshing(true);
            pGlobal = null;
        }

        /**
         * Comunicacion asincrona
         * @param e void
         * @return ArrayAdapter
         */
        protected ArrayAdapter doInBackground(Void... e) {
            // Eliminamos el snackbar anterior si no esta elinimado
            if (snackbar != null) snackbar.dismiss();

            // Obtiene del BBDD remoto las programaciones destacadas
            List<HashMap<String, String>> programacion = clienteRest.buscar(query,"",0,0,0);

            // Si no se ha podido establecer la conexion
            if (programacion == null) return null;
            String titulo;
            String bar;
            for (int i = 0; i < programacion.size(); i++) {
                titulo = programacion.get(i).get("Titulo");
                bar = programacion.get(i).get("Bar");
                if (mDbHelper.comprobarFavorito(titulo,bar)) {
                    programacion.get(i).put("CheckBox", "1");
                } else {
                    programacion.get(i).put("CheckBox", "0");
                }
            }
            String[] from = new String[] { "Titulo", "Categoria", "Bar", "Descr", "Inicio", "Fin","CheckBox"};

            int[] to = new int[] { R.id.titulo , R.id.categoria, R.id.bar, R.id.descr,
                    R.id.inicio, R.id.fin,R.id.favCheck};
            hayBusqueda=true;
            ultimaBusqueda=query;
            if(query.contains("Bar:")){
                String realQuery = query.split("Bar:")[1];
                programacion=clienteRest.getProgramacionDadoBar(realQuery);
            }

            pGlobal = programacion;
            ListHashAdapter res = new ListHashAdapter(getActivity(), R.layout.busqueda_listview_content,
                    programacion, from, to);
            if(hayFiltro){
                quitarFiltros();
                List<HashMap<String, String>> programacionFiltrada;
                if(!filtroCategoria.isEmpty()) {
                    if(hayFiltroFecha) {
                        programacionFiltrada = clienteRest.buscar(query, filtroCategoria, filtroFechaDia,
                                filtroFechaMes, filtroFechaAño);
                    }else {
                        programacionFiltrada = clienteRest.buscar(query,filtroCategoria,0,0,0);
                    }
                    // Configurar el adapter
                    pGlobal = programacionFiltrada;
                    res = new ListHashAdapter(getActivity(), R.layout.busqueda_listview_content,
                            programacionFiltrada, from, to);
                } else if(hayFiltroFecha){
                    programacionFiltrada=clienteRest.buscar(query,"",filtroFechaDia,filtroFechaMes,filtroFechaAño);
                    pGlobal = programacionFiltrada;
                    res = new ListHashAdapter(getActivity(), R.layout.busqueda_listview_content,
                            programacionFiltrada, from, to);
                }

            }
            return res;
        }
        /**
         * Una vez obtenido los datos, se rellena el listview
         * @param adapter
         */
        protected void onPostExecute(ArrayAdapter adapter) {
            noResText.setVisibility(View.GONE);
            if (adapter == null) {
                try {
                    // Mensaje error en caso de no poder conectar con la BBDD
                    snackbar = Snackbar.make(getView(), R.string.error_conexion, Snackbar.LENGTH_INDEFINITE)
                            .setAction("Action", null);
                    snackbar.show();
                } catch (Exception x) { x.printStackTrace();}
                swipeRefreshLayout.setRefreshing(false);
            } else {
                if(adapter.isEmpty()){
                    noResText.setVisibility(View.VISIBLE);
                }
                searchView.setQuery(query,false);
                adapterGlobal = adapter;
                mList.setAdapter(adapterGlobal);
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    //SECCION DE COSAS ANTIGUAS QUE NO QUIERO BORRAR POR SI LA LIAMOS

    /*Funcion antigua de busqueda, por si acaso*/

//    public ArrayAdapter busqueda(String query){
//        List<HashMap<String, String>> programacion = clienteRest.getProgramacionDadoBar("Dantis");
//        String[] from = new String[] { "Titulo", "Categoria", "Bar", "Descr", "Inicio", "Fin"};
//
//        int[] to = new int[] { R.id.titulo , R.id.categoria, R.id.bar, R.id.descr,
//                R.id.inicio, R.id.fin};
//        hayBusqueda=true;
//        ultimaBusqueda=query;
//        ListHashAdapter res = new ListHashAdapter(this.getActivity(), R.layout.destacado_listview_content,
//                programacion, from, to);
//        if(hayFiltro){
//            quitarFiltros();
//            List<HashMap<String, String>> programacionFiltrada = new ArrayList<HashMap<String, String>>();
//            if(!filtroCategoria.isEmpty()) {
//                for (HashMap<String, String> e : programacion) {
//                    if (e.containsValue(filtroCategoria)) {
//                        programacionFiltrada.add(e);
//                    }
//                }
//                // Configurar el adapter
//                res = new ListHashAdapter(this.getActivity(), R.layout.destacado_listview_content,
//                        programacionFiltrada, from, to);
//            }else if(hayFiltroFecha){
//                List<HashMap<String, String>> programacionConFecha = new ArrayList<HashMap<String, String>>();
//                if(programacionFiltrada.isEmpty()){
//                    programacionFiltrada=programacion;
//                }
//                for (HashMap<String, String> e : programacionFiltrada) {
//                    String inicio=e.get("Inicio");
//                    String fin=e.get("Fin");
//                    String fechaInicio=inicio.split(" ")[0];
//                    String fechaFin=fin.split(" ")[0];
//                    e.put("FechaInicio",fechaInicio);
//                    e.put("FechaFin",fechaFin);
//                    programacionConFecha.add(e);
//                }
//                List<HashMap<String, String>> programacionFiltrada2 = new ArrayList<HashMap<String, String>>();
//                for (HashMap<String, String> e : programacionConFecha) {
//                    if (e.containsValue(filtroFechaAño+"-"+
//                            filtroFechaMes+"-"+
//                            filtroFechaDia)) {
//                        programacionFiltrada2.add(e);
//                    }
//                }
//                res = new ListHashAdapter(this.getActivity(), R.layout.destacado_listview_content,
//                        programacionFiltrada2, from, to);
//            }
//
//        }
//        return res;
//    }

//    public ArrayAdapter progProx(){
//        // Obtiene del BD remoto las programaciones destacadas
//        List<HashMap<String, String>> programacion = clienteRest.getProgramacion();
//        hayBusqueda=false;
//        // Crear un array donde se especifica los datos que se quiere mostrar
//        String[] from = new String[] { "Titulo", "Categoria", "Bar", "Descr", "Inicio", "Fin"};
//
//        // Crear un array done sse especifica los campos de ListView que se quiere rellenar
//        int[] to = new int[] { R.id.titulo , R.id.categoria, R.id.bar, R.id.descr,
//                R.id.inicio, R.id.fin};
//        ListHashAdapter res = new ListHashAdapter(this.getActivity(), R.layout.destacado_listview_content,
//                programacion, from, to);
//        if(hayFiltro){
//            quitarFiltros();
//            List<HashMap<String, String>> programacionFiltrada = new ArrayList<HashMap<String, String>>();
//            if(!filtroCategoria.isEmpty()) {
//                for (HashMap<String, String> e : programacion) {
//                    if (e.containsValue(filtroCategoria)) {
//                        programacionFiltrada.add(e);
//                    }
//                }
//                // Configurar el adapter
//                res =  new ListHashAdapter(this.getActivity(), R.layout.destacado_listview_content,
//                        programacionFiltrada, from, to);
//            }
//        }
//        return res;
//    }
}

