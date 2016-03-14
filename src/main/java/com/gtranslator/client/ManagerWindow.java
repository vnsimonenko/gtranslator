package com.gtranslator.client;

import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.utils.SpringApplicationContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.w3c.dom.Document;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLInputElement;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;

public class ManagerWindow extends Application implements PropertyChangeListener {

    static {
        URL.setURLStreamHandlerFactory(protocol -> {
            return "resource".equals(protocol) ? new URLStreamHandler() {
                protected URLConnection openConnection(URL url) throws IOException {
                    return new ResourceURLConnection(url);
                }
            } : null; //default handler
        });
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Value(value = "${custom.prop.src.lang}")
    private String srcLang;

    @Value(value = "${custom.prop.trg.lang}")
    private String trgLang;

    @Value(value = "${custom.prop.amount_view_words}")
    private String textAmountViewWords;

    @Value(value = "${custom.prop.auto.play.am}")
    private Boolean amAutoPlay;

    @Value(value = "${custom.prop.auto.play.br}")
    private Boolean brAutoPlay;

    @Value(value = "${custom.prop.mode}")
    private String mode;

    private volatile boolean enabled = true;

    private ObservableList<History> histories = FXCollections.synchronizedObservableList(FXCollections.<History>observableArrayList());
    private ObservableList<History> historyTargets = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private static int WIDTH = 230;
    private static int HEIGHT = 450;

    public ManagerWindow() {
    }

    private Scene scene;

    public void init() throws Exception {
        SpringApplicationContext.getContext().getAutowireCapableBeanFactory().autowireBean(this);
        com.gtranslator.Application.PROPERTYSUPPORT.addPropertyChangeListener(this);
    }

    public void stop() throws Exception {
        com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(com.gtranslator.Application.PropertySupport.Property.EXIT,
                false, true);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Google translator");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/client/images/fish.png")));

        Tab setupTab = new Tab("Setup");
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/client/images/setup.png")));
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        setupTab.setGraphic(imageView);
        setupTab.setContent(new Browser(WIDTH, HEIGHT));
        setupTab.setClosable(false);
        Tab historyTab = new Tab("History");
        imageView = new ImageView(new Image(getClass().getResourceAsStream("/client/images/history.png")));
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        historyTab.setGraphic(imageView);
        historyTab.setContent(createHistoryNode());
        historyTab.setClosable(false);
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                setupTab,
                historyTab
        );

        scene = new Scene(tabPane, WIDTH, HEIGHT, Color.web("#666970"));
        stage.setScene(scene);
        scene.getStylesheets().add("/client/css/BrowserToolbar.css");

        com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                com.gtranslator.Application.PropertySupport.Property.MODE,
                null, mode);

        stage.show();
        stage.centerOnScreen();
    }

    public static void launch(String[] args) {
        Application.launch(args);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (com.gtranslator.Application.PropertySupport.Property.HISTORY.equalsEvent(evt)) {
            Platform.runLater(() -> {
                Map.Entry<Lang, DictionaryModel> entry = (Map.Entry<Lang, DictionaryModel>) evt.getNewValue();
                DictionaryModel model = entry.getValue();
                StringBuilder targets = new StringBuilder();
                int i = 5;
                Set<String> categs = new HashSet<>();
                for (DictionaryModel.TranslationRecord t : model.getTranslationRecords()) {
                    if (t.getLang() == entry.getKey()) {
                        if (i-- < 0) {
                            break;
                        }
                        if (!categs.contains(t.getCategory())) {
                            categs.add(t.getCategory());
                            targets.append(t.getTranslation());
                            targets.append(";");
                        }
                    }
                }
                if (targets.length() == 0) {
                    return;
                }
                History history;
                synchronized (histories) {
                    Iterator<History> it = histories.iterator();
                    if (it.hasNext()) {
                        history = it.next();
                        history.add(model.getSourceLang(), entry.getKey(), model.getSource(), targets.substring(0, targets.length() - 1).toString());
                    } else {
                        history = History.createNew(model.getSourceLang(), entry.getKey(), model.getSource(), targets.toString());
                    }
                }
                histories.clear();
                historyTargets.clear();
                histories.addAll(history.getLangs());
                historyTargets.addAll(history.getByLang());
            });
        } else if (com.gtranslator.Application.PropertySupport.Property.SRC_LANG.equalsEvent(evt)) {
            srcLang = ((Lang) evt.getNewValue()).name().toLowerCase();
        } else if (com.gtranslator.Application.PropertySupport.Property.TRG_LANG.equalsEvent(evt)) {
            trgLang = ((Lang) evt.getNewValue()).name().toLowerCase();
        } else if (com.gtranslator.Application.PropertySupport.Property.AMOUNT_VIEW_WORDS.equalsEvent(evt)) {
            textAmountViewWords = "" + evt.getNewValue();
        }
    }

    class Browser extends Region {
        private int width;
        private int height;
        private final WebView browser = new WebView();
        private final WebEngine webEngine = browser.getEngine();

        public Browser(int width, int height) {
            this.width = width;
            this.height = height;
            getStyleClass().add("browser");

            webEngine.getLoadWorker().stateProperty().addListener(
                    new ChangeListener<State>() {
                        @Override
                        public void changed(ObservableValue<? extends State> ov,
                                            State oldState, State newState) {
                            if (newState == State.SUCCEEDED) {
                                JSObject win =
                                        (JSObject) webEngine.executeScript("window");
                                win.setMember("app", new JavaApp());
                            }
                        }
                    }
            );

            webEngine.getLoadWorker().stateProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (newValue == State.SUCCEEDED) {
                            try {
                                ManagerWindow.this.srcLang = "en";
                                ManagerWindow.this.trgLang = "ru";
                                ManagerWindow.this.textAmountViewWords = "10";
                                ElementHelper elh = new ElementHelper(webEngine.getDocument());

                                HTMLInputElement el = elh.getElementById(ManagerWindow.this.srcLang + "1");
                                if (el != null) {
                                    el.setChecked(true);
                                    el = elh.getElementById(ManagerWindow.this.trgLang + "2");
                                    el.setChecked(true);
                                    el = elh.getElementById("amountwords");
                                    el.setValue(ManagerWindow.this.textAmountViewWords);
                                    elh.addEventListener("amountwords_but", "click", evt -> {
                                        HTMLInputElement amount = elh.getElementById("amountwords");
                                        try {
                                            com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                                                    com.gtranslator.Application.PropertySupport.Property.AMOUNT_VIEW_WORDS,
                                                    0, Integer.valueOf(amount.getValue()));
                                        } catch (NumberFormatException ex) {
                                            //
                                        }
                                    }, false);
                                    //type of translation
                                    for (String id : new String[] {"en1", "ru1", "ua1", "en2", "ru2", "ua2"}) {
                                        elh.addEventListener(id, "change", evt -> {
                                            com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                                                    id.endsWith("1")
                                                    ? com.gtranslator.Application.PropertySupport.Property.SRC_LANG
                                                    : com.gtranslator.Application.PropertySupport.Property.TRG_LANG,
                                                    null, Lang.valueOf(id.substring(0, id.length() - 1).toUpperCase()));
                                        }, false);
                                    }
                                    // mode
                                    org.w3c.dom.events.EventListener listener = evt -> {
                                        HTMLInputElement elt = HTMLInputElement.class.cast(evt.getTarget());
                                        com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                                                com.gtranslator.Application.PropertySupport.Property.MODE,
                                                null, elt.getValue());
                                    };
                                    el = elh.getElementById("copyid");
                                    el.setChecked(ManagerWindow.this.mode.startsWith("copy"));
                                    elh.addEventListener(el, "change", listener, false);
                                    el = elh.getElementById("selectid");
                                    el.setChecked(!ManagerWindow.this.mode.startsWith("copy"));
                                    elh.addEventListener(el, "change", listener, false);
                                    //play
                                    listener = evt -> {
                                        HTMLInputElement elt = HTMLInputElement.class.cast(evt.getTarget());
                                        com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                                                com.gtranslator.Application.PropertySupport.Property.valueOf(elt.getValue()),
                                                null, elt.getChecked());
                                    };
                                    el = elh.getElementById("playamid");
                                    el.setChecked(ManagerWindow.this.amAutoPlay);
                                    elh.addEventListener(el, "change", listener, false);
                                    el = elh.getElementById("playbrid");
                                    el.setChecked(ManagerWindow.this.brAutoPlay);
                                    elh.addEventListener(el, "change", listener, false);
                                    el = elh.getElementById("enabledid");
                                    el.setChecked(ManagerWindow.this.enabled);
                                    listener = evt -> {
                                        HTMLInputElement elt = HTMLInputElement.class.cast(evt.getTarget());
                                        enabled = elt.getChecked();
                                    };
                                    elh.addEventListener(el, "change", listener, false);
                                }
                                //browser.webEngine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
                            } catch (JSException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

            setOnMouseEntered(event -> com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                    com.gtranslator.Application.PropertySupport.Property.ACTIVE,
                    true, false));
            setOnMouseExited(event -> com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                    com.gtranslator.Application.PropertySupport.Property.ACTIVE,
                    false, enabled ? true : false));

            // load the home page
            webEngine.load(getClass().getResource("/client/app.html").toExternalForm());
            getChildren().add(browser);
        }

        public class JavaApp {
            public void exit() {
                com.gtranslator.Application.PROPERTYSUPPORT.firePropertyChange(
                        com.gtranslator.Application.PropertySupport.Property.EXIT,
                        false, true
                );
            }
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            layoutInArea(browser, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
        }

        @Override
        protected double computePrefWidth(double height) {
            return this.width;
        }

        @Override
        protected double computePrefHeight(double width) {
            return this.height;
        }
    }

    @EventListener({ContextClosedEvent.class})
    public void close() {
    }

    public static class History {
        private Lang srcLang = Lang.EN;
        private Lang trgLang = Lang.EN;
        private String source = "";
        private String target = "";
        private History next;

        private History(Lang srcLang, Lang trgLang, String source, String target) {
            this.srcLang = srcLang;
            this.trgLang = trgLang;
            this.source = source;
            this.target = target;
        }

        public Lang getSrcLang() {
            return srcLang;
        }

        public void setSrcLang(Lang srcLang) {
            this.srcLang = srcLang;
        }

        public Lang getTrgLang() {
            return trgLang;
        }

        public void setTrgLang(Lang trgLang) {
            this.trgLang = trgLang;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getLang() {
            return srcLang.name() + "-" + trgLang.name();
        }

        public void setLang(String lang) {
        }

        public Set<History> getLangs() {
            Set<String> keys = new LinkedHashSet<>();
            Set<History> hs = new LinkedHashSet<>();
            History history = this;
            do {
                String key = history.getLang();
                if (!keys.contains(key)) {
                    hs.add(history);
                    keys.add(key);
                }
                history = history.next;
            } while (history != null && history != this);
            return hs;
        }

        public List<History> getByLang() {
            List<History> hs = new ArrayList<>();
            History history = this;
            do {
                if (srcLang == history.srcLang && trgLang == history.trgLang) {
                    hs.add(history);
                }
                history = history.next;
            } while (history != null && history != this);
            return hs;
        }

        public static History createNew(Lang srcLang, Lang trgLang, String source, String target) {
            return new History(srcLang, trgLang, source, target);
        }

        public void add(Lang srcLang, Lang trgLang, String source, String target) {
            History history = History.createNew(srcLang, trgLang, source, target);
            history.next = next;
            next = history;
        }
    }

    private Node createHistoryNode() {
        GridPane gridpane = new GridPane();
        gridpane.setPadding(new Insets(5));
        gridpane.setHgap(10);
        gridpane.setVgap(10);

        Label sourceLbl = new Label("Language");
        GridPane.setHalignment(sourceLbl, HPos.CENTER);
        gridpane.add(sourceLbl, 0, 0);

        //fillHistories();
        final ListView<History> headHistoryListView = new ListView<>(histories);
        headHistoryListView.setPrefWidth(150);
        headHistoryListView.setMinWidth(150);
        headHistoryListView.setPrefHeight(150);

        headHistoryListView.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    //historyTargets.clear();
                }
            }
        });

        headHistoryListView.setCellFactory(new Callback<ListView<History>, ListCell<History>>() {
            @Override
            public ListCell<History> call(ListView<History> param) {
                return new ListCell<History>() {
                    @Override
                    public void updateItem(History item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getSrcLang() + " - " + item.getTrgLang());
                            setTooltip(new Tooltip(item.getSrcLang() + " " + item.getTrgLang()));
                        }
                    }
                };
            }
        });

        gridpane.add(headHistoryListView, 0, 1);

        Label targetLbl = new Label("Translation");
        gridpane.add(targetLbl, 2, 0);
        GridPane.setHalignment(targetLbl, HPos.CENTER);

        final TableView<History> targetTableView = new TableView<>();
        targetTableView.setPrefWidth(300);
        targetTableView.setMaxWidth(Integer.MAX_VALUE);

        targetTableView.setItems(historyTargets);

        TableColumn<History, String> langNameCol = new TableColumn<>("Lang");
        langNameCol.setEditable(true);
        langNameCol.setCellValueFactory(new PropertyValueFactory("lang"));
        langNameCol.setPrefWidth(targetTableView.getPrefWidth() / 8);

        TableColumn<History, String> sourceNameCol = new TableColumn<>("Source");
        sourceNameCol.setEditable(true);
        sourceNameCol.setCellValueFactory(new PropertyValueFactory("source"));
        sourceNameCol.setPrefWidth(targetTableView.getPrefWidth() * 2 / 8);

        TableColumn<History, String> targetNameCol = new TableColumn<>("Target");
        targetNameCol.setCellValueFactory(new PropertyValueFactory("Target"));
        targetNameCol.setPrefWidth(targetTableView.getPrefWidth() * 5 / 8);

        targetTableView.getColumns().setAll(langNameCol, sourceNameCol, targetNameCol);
        gridpane.add(targetTableView, 2, 1);

        headHistoryListView.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends
                History> observable, History oldValue, History newValue) -> {
            if (observable != null && observable.getValue() != null) {
                historyTargets.clear();
                historyTargets.addAll(observable.getValue().getByLang());
            }
        });

        BorderPane root = new BorderPane() {
            @Override
            protected void layoutChildren() {
                double w = getWidth();
                double h = getHeight();
                double offset = targetTableView.getPadding().getLeft() + targetTableView.getInsets().getLeft();
                layoutInArea(gridpane, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
                targetTableView.setPrefWidth(w - headHistoryListView.getPrefWidth() - 2 * gridpane.getHgap() - 2 * gridpane.getInsets().getLeft());
                headHistoryListView.setPrefHeight(h - sourceLbl.getPrefHeight());
                targetTableView.setPrefHeight(h - sourceLbl.getPrefHeight());
                langNameCol.setPrefWidth((targetTableView.getPrefWidth() - offset) / 8);
                sourceNameCol.setPrefWidth((targetTableView.getPrefWidth() - offset) * 2 / 8);
                targetNameCol.setPrefWidth((targetTableView.getPrefWidth() - offset) * 5 / 8);
            }
        };
        root.setCenter(gridpane);
        return root;
    }

//    private void fillHistories() {
//        History root = History.createNew(Lang.EN, Lang.RU, "test1", "тест1");
//        root.add(Lang.EN, Lang.RU, "test2", "тест2");
//        root.add(Lang.EN, Lang.RU, "test3", "тест3");
//        root.add(Lang.EN, Lang.RU, "test4", "тест4");
//        root.add(Lang.RU, Lang.EN, "test1", "тест5");
//        root.add(Lang.RU, Lang.EN, "test2", "тест6");
//        root.add(Lang.UA, Lang.RU, "test1", "тест7");
//        root.add(Lang.UA, Lang.RU, "test2", "тест8");
//        histories.addAll(root.getLangs());
//    }

    class ElementHelper {
        private Document doc;

        public ElementHelper(Document doc) {
            this.doc = doc;
        }

        <T> T getElementById(String id) {
            return (T) doc.getElementById(id);
        }

        void addEventListener(String id, String eventType, org.w3c.dom.events.EventListener listener, boolean useCapture) {
            ((EventTarget) doc.getElementById(id)).addEventListener(eventType, listener, useCapture);
        }

        void addEventListener(final Object obj, String eventType, org.w3c.dom.events.EventListener listener, boolean useCapture) {
            ((EventTarget) obj).addEventListener(eventType, listener, useCapture);
        }
    }

    public static void main(String... args) {
        Application.launch(args);
    }
}