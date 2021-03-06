package com.wakatime.android.dashboard.project;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wakatime.android.R;
import com.wakatime.android.WakatimeApplication;
import com.wakatime.android.dashboard.model.Project;
import com.wakatime.android.support.JsonParser;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.support.v4.content.ContextCompat.getColor;

/**
 * Project data fragment
 *
 * @author Joao Pedro Evangelista
 */
public class ProjectFragment extends Fragment implements ViewModel,
    SearchView.OnQueryTextListener, SearchView.OnCloseListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String KEY = "project-fragment";

    private static final String ROTATION_CACHE = "rotation-cache";

    @BindView(R.id.recycler_projects)
    RecyclerView mRecyclerProjects;

    @BindView(R.id.loader_projects)
    SpinKitView mLoaderProjects;

    @BindView(R.id.chart_projects)
    PieChart mChartProjects;

    @BindView(R.id.nested_projects)
    NestedScrollView mNestedProjects;

    @BindView(R.id.swipe_container)
    SwipeRefreshLayout mRefreshLayout;
    @Inject
    ProjectPresenter mPresenter;
    private OnProjectFragmentInteractionListener mListener;
    private List<Project> rotationCache;

    private Tracker mTracker;
    private ProjectAdapter mAdapter;


    public ProjectFragment() {
        // Required empty public constructor
    }

    /**
     * @return A new instance of fragment ProjectFragment.
     */
    public static ProjectFragment newInstance() {
        return new ProjectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WakatimeApplication application = (WakatimeApplication) this.getActivity().getApplication();
        application.useDashboardComponent().inject(this);
        mTracker = application.getTracker();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnProjectFragmentInteractionListener) {
            this.mListener = ((OnProjectFragmentInteractionListener) context);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnProjectFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(ROTATION_CACHE)) {
            this.rotationCache = JsonParser.read(savedInstanceState.getString(ROTATION_CACHE),
                    new TypeReference<List<Project>>() {
                    });
            this.setProjects(this.rotationCache);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_project, container, false);
        ButterKnife.bind(this, view);
        mPresenter.bind(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mRefreshLayout.setOnRefreshListener(this);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorDarkAccent);

        if (savedInstanceState == null || !savedInstanceState.containsKey(ROTATION_CACHE)) {
            mPresenter.onInit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mTracker.setScreenName("Dashboard-Projects");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ROTATION_CACHE, JsonParser.write(this.rotationCache));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search, menu);

        final MenuItem searchAction = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchAction);

        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.onFinish();
        mPresenter.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener = null;
    }


    @Override
    public void setProjects(List<Project> projects) {
        setChartData(projects);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new ProjectAdapter(getActivity(), projects, mListener);
        mRecyclerProjects.setLayoutManager(layoutManager);
        mRecyclerProjects.setAdapter(mAdapter);
    }

    @Override
    public void setRotationCache(List<Project> projects) {
        this.rotationCache = projects;
    }

    @Override
    public void completeRefresh() {
        this.mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void hideLoader() {
        this.mLoaderProjects.setVisibility(View.GONE);
        this.mNestedProjects.setVisibility(View.VISIBLE);
    }

    @Override
    public void showLoader() {
        this.mLoaderProjects.setVisibility(View.VISIBLE);
        this.mNestedProjects.setVisibility(View.GONE);
    }

    private void setChartData(List<Project> chardData) {
        Typeface lato = Typeface.createFromAsset(this.getContext().getAssets(), "fonts/Lato-Regular.ttf");
        this.mChartProjects.setDrawHoleEnabled(true);
        this.mChartProjects.setHoleColor(Color.WHITE);
        this.mChartProjects.setTransparentCircleColor(Color.WHITE);
        this.mChartProjects.setTransparentCircleAlpha(110);
        this.mChartProjects.setDragDecelerationFrictionCoef(0.95f);
        this.mChartProjects.setHoleRadius(58f);
        this.mChartProjects.setTransparentCircleRadius(61f);
        this.mChartProjects.setDescription("");
        this.mChartProjects.setUsePercentValues(true);
        this.mChartProjects.setEntryLabelColor(Color.WHITE);
        this.mChartProjects.setDrawCenterText(true);
        this.mChartProjects.setCenterText(getString(R.string.title_projects));
        this.mChartProjects.setCenterTextSize(18f);
        this.mChartProjects.setEntryLabelTypeface(lato);
        this.mChartProjects.setCenterTextTypeface(lato);
        this.mChartProjects.setCenterTextColor(getColor(getActivity(), R.color.colorSecondaryText));
        this.mChartProjects.animateY(1400, Easing.EasingOption.EaseInOutQuad);
        enableNestingScrollForAP21();

        List<PieEntry> entries = new ArrayList<>(chardData.size());
        for (Project project : chardData) {
            entries.add(new PieEntry(project.getPercent(), project.getName()));
        }
        PieDataSet pieDataSet = new PieDataSet(entries, getString(R.string.title_projects));
        pieDataSet.setHighlightEnabled(true);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(14f);
        pieDataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(5f);
        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter());
        this.mChartProjects.setData(pieData);
    }

    private void enableNestingScrollForAP21() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mChartProjects.setNestedScrollingEnabled(true);
        }
    }

    @Override
    public void notifyError(Throwable error) {
        Snackbar snackbar = Snackbar.make(mRefreshLayout,
                R.string.could_not_fetch, Snackbar.LENGTH_LONG);

        snackbar.setAction(R.string.retry, view -> {
            mPresenter.onInit();
            snackbar.dismiss();
        });

        snackbar.show();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return filter(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return filter(newText);
    }

    private boolean filter(String query) {
        if (mAdapter != null) {
            List<Project> projects = mAdapter.filter(query);
            mAdapter.animateTo(projects);
            mRecyclerProjects.scrollToPosition(0);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onClose() {
        filter("");
        return false;
    }

    @Override
    public void onRefresh() {
        this.mPresenter.onRefresh();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnProjectFragmentInteractionListener {

        void showProjectPage(Project project);
    }
}
