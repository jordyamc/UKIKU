package knf.kuma.favorite;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.BottomFragment;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.favorite.objects.InfoContainer;
import knf.kuma.pojos.FavSection;
import knf.kuma.pojos.FavoriteObject;
import xdroid.toaster.Toaster;

public class FavoriteFragment extends BottomFragment implements FavsSectionAdapter.OnMoveListener {

    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.error)
    LinearLayout error_layout;
    private FavoriteObject edited;
    private RecyclerView.LayoutManager manager;
    private FavsSectionAdapter adapter;
    private boolean isFirst = true;

    private int count = 0;

    public FavoriteFragment() {
    }

    public static FavoriteFragment get() {
        return new FavoriteFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CacheDB.INSTANCE.favsDAO().getAll().observe(this, list -> FavSectionHelper.reload());
        ViewModelProviders.of(getActivity()).get(FavoriteViewModel.class).getData(getContext()).observe(this, favoriteObjects -> {
            if (favoriteObjects == null || favoriteObjects.size() == 0) {
                error_layout.setVisibility(View.VISIBLE);
                adapter.updateList(new ArrayList<>());
            } else if (PrefsUtil.showFavSections()) {
                error_layout.setVisibility(View.GONE);
                InfoContainer container = FavSectionHelper.getInfoContainer(edited);
                if (container.needReload) {
                    adapter.updateList(favoriteObjects);
                    if (isFirst) {
                        isFirst = false;
                        recyclerView.scheduleLayoutAnimation();
                    }
                } else
                    adapter.updatePosition(container);
            } else {
                error_layout.setVisibility(View.GONE);
                adapter.updateList(favoriteObjects);
                if (isFirst) {
                    isFirst = false;
                    recyclerView.scheduleLayoutAnimation();
                }
            }
            edited = null;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        ButterKnife.bind(this, view);
        manager = recyclerView.getLayoutManager();
        adapter = new FavsSectionAdapter(this, recyclerView);
        if (PrefsUtil.getLayType().equals("1") && PrefsUtil.showFavSections()) {
            ((GridLayoutManager) manager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    try {
                        return FavSectionHelper.getCurrentList().get(position).isSection ?
                                ((GridLayoutManager) manager).getSpanCount() :
                                1;
                    } catch (Exception e) {
                        return 1;
                    }
                }
            });
        }
        recyclerView.setAdapter(adapter);
        EAHelper.enter1(getContext(), "F");
        return view;
    }

    @LayoutRes
    private int getLayout() {
        if (PrefsUtil.getLayType().equals("0")) {
            return R.layout.recycler_favs;
        } else {
            return R.layout.recycler_favs_grid;
        }
    }

    public void onChangeOrder() {
        if (getActivity() != null)
            ViewModelProviders.of(getActivity()).get(FavoriteViewModel.class).getData(getContext()).observe(this, favoriteObjects -> {
                if (favoriteObjects == null || favoriteObjects.size() == 0) {
                    adapter.updateList(new ArrayList<>());
                    error_layout.post(() -> error_layout.setVisibility(View.VISIBLE));
                } else {
                    adapter.updateList(favoriteObjects);
                    if (isFirst) {
                        isFirst = false;
                        recyclerView.scheduleLayoutAnimation();
                    }
                }
            });
    }

    public void showNewCategoryDialog(FavoriteObject object) {
        edited = object;
        showNewCategoryDialog(object == null, null);
    }

    private void showNewCategoryDialog(boolean isEmpty, @Nullable String name) {
        List<String> categories = FavoriteObject.getCategories(CacheDB.INSTANCE.favsDAO().getCatagories());
        new MaterialDialog.Builder(getContext())
                .title("Nueva categoría")
                .input("Nombre", name, false, (dialog, input) -> {
                    if (categories.contains(input.toString())) {
                        Toaster.toast("Esta categoría ya existe");
                        showNewCategoryDialog(isEmpty, name);
                    } else {
                        if (isEmpty)
                            showNewCategoryInit(false, input.toString());
                        else {
                            edited.setCategory(input.toString());
                            CacheDB.INSTANCE.favsDAO().addFav(edited);
                            edited = null;
                        }
                    }
                })
                .positiveText("crear")
                .negativeText("cancelar")
                .build().show();

    }

    public void showNewCategoryInit(boolean isEdit, String name) {
        String f_name = name.equals("Sin categoría") ? FavoriteObject.CATEGORY_NONE : name;
        List<FavoriteObject> favoriteObjects = CacheDB.INSTANCE.favsDAO().getNotInCategory(f_name);
        if (favoriteObjects.size() == 0) {
            Toaster.toast("Necesitas favoritos para crear una categoría");
        } else {
            MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext())
                    .title(name)
                    .items(FavoriteObject.getNames(favoriteObjects))
                    .itemsCallbackMultiChoice(null, (dialog, which, text) -> {
                        edited = null;
                        List<FavoriteObject> list = new ArrayList<>();
                        for (int i : which) {
                            FavoriteObject object = favoriteObjects.get(i);
                            object.setCategory(f_name);
                            list.add(object);
                        }
                        CacheDB.INSTANCE.favsDAO().addAll(list);
                        return false;
                    })
                    .positiveText("agregar");
            if (isEdit && !f_name.equals(FavoriteObject.CATEGORY_NONE))
                builder.negativeText("cancelar")
                        .neutralText("eliminar")
                        .onNeutral((dialog, which) ->
                                new MaterialDialog.Builder(getContext())
                                        .content("¿Desea eliminar esta categoría?")
                                        .positiveText("continuar")
                                        .onPositive((dialog1, which1) -> {
                                            edited = null;
                                            List<FavoriteObject> objects = CacheDB.INSTANCE.favsDAO().getAllInCategory(f_name);
                                            for (FavoriteObject object : objects) {
                                                object.setCategory(FavoriteObject.CATEGORY_NONE);
                                            }
                                            CacheDB.INSTANCE.favsDAO().addAll(objects);
                                        })
                                        .negativeText("cancelar")
                                        .build().show());
            else if (!isEdit)
                builder.negativeText("atras")
                        .onNegative((dialog, which) -> showNewCategoryDialog(true, name))
                        .cancelListener(dialog -> showNewCategoryDialog(true, name));
            builder.build().show();
        }
    }

    @Override
    public void onEdit(String category) {
        showNewCategoryInit(true, category);
    }

    @Override
    public void onSelect(FavoriteObject object) {
        if (!(object instanceof FavSection)) {
            List<String> categories = FavoriteObject.getCategories(CacheDB.INSTANCE.favsDAO().getCatagories());
            if (categories.size() <= 1) {
                edited = object;
                showNewCategoryDialog(false, null);
            } else {
                new MaterialDialog.Builder(getContext())
                        .title("Mover a...")
                        .items(categories)
                        .itemsCallbackSingleChoice(categories.indexOf(object.category), (dialog, itemView, which, text) -> {
                            if (text != null && !text.equals(object.category)) {
                                edited = object;
                                edited.setCategory(text.toString().equals("Sin categoría") ? "_NONE_" : text.toString());
                                CacheDB.INSTANCE.favsDAO().addFav(edited);
                            } else
                                Toaster.toast("Error al mover");
                            return false;
                        })
                        .positiveText("mover")
                        .negativeText("cancelar")
                        .neutralText("nuevo")
                        .onNeutral((dialog, which) -> {
                            edited = object;
                            showNewCategoryDialog(false, null);
                        }).build().show();
            }
        }
    }

    @Override
    public void onReselect() {
        EAHelper.enter1(getContext(), "F");
        if (manager != null) {
            manager.smoothScrollToPosition(recyclerView, null, 0);
            count++;
            if (count == 3) {
                if (adapter != null)
                    Toaster.toast("Tienes " + adapter.getItemCount() + " animes en favoritos");
                count = 0;
            }

        }
    }
}
