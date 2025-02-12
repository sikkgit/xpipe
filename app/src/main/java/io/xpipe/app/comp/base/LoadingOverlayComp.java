package io.xpipe.app.comp.base;

import atlantafx.base.controls.RingProgressIndicator;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;

public class LoadingOverlayComp extends Comp<CompStructure<StackPane>> {

    public static LoadingOverlayComp noProgress(Comp<?> comp, ObservableValue<Boolean> loading) {
        return new LoadingOverlayComp(comp, loading, new SimpleDoubleProperty(-1));
    }

    private final Comp<?> comp;
    private final ObservableValue<Boolean> showLoading;
    private final ObservableValue<Number> progress;

    public LoadingOverlayComp(Comp<?> comp, ObservableValue<Boolean> loading, ObservableValue<Number> progress) {
        this.comp = comp;
        this.showLoading = PlatformThread.sync(loading);
        this.progress = PlatformThread.sync(progress);
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var compStruc = comp.createStructure();
        var r = compStruc.get();

        var loading = new RingProgressIndicator(0, false);
        loading.progressProperty().bind(progress);
        loading.visibleProperty().bind(Bindings.not(AppPrefs.get().performanceMode()));

        var loadingOverlay = new StackPane(loading);
        loadingOverlay.getStyleClass().add("loading-comp");
        loadingOverlay.setVisible(showLoading.getValue());

        var listener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean busy) {
                if (!busy) {
                    // Reduce flickering for consecutive loads
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (!showLoading.getValue()) {
                            Platform.runLater(() -> loadingOverlay.setVisible(false));
                        }
                    });
                } else {
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (showLoading.getValue()) {
                            Platform.runLater(() -> loadingOverlay.setVisible(true));
                        }
                    });
                }
            }
        };
        showLoading.addListener(listener);

        var stack = new StackPane(r, loadingOverlay);

        loading.prefWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return Math.min(r.getHeight() - 20, 50);
                        },
                        r.heightProperty()));
        loading.prefHeightProperty().bind(loading.prefWidthProperty());

        return new SimpleCompStructure<>(stack);
    }
}
