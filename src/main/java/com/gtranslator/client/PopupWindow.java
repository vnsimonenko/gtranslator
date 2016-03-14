package com.gtranslator.client;

import com.gtranslator.Application;
import com.gtranslator.BaseException;
import com.gtranslator.cloud.TranslateService;
import com.gtranslator.storage.domain.DictionaryModel;
import com.gtranslator.storage.domain.Lang;
import com.gtranslator.storage.domain.Phonetic;
import com.gtranslator.utils.AudioHelper;
import com.gtranslator.utils.Utils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.ContextClosedEvent;

import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@org.springframework.stereotype.Component
public class PopupWindow implements CommandLineRunner, PropertyChangeListener {
    final static Logger logger = LoggerFactory.getLogger(PopupWindow.class);
    final Point currentScreenPoint = new Point();
    private JEditorPane viewer;
    private JPanel playPanel;
    private JPopupMenu menu;
    private JFrame popupFrame;
    private JMenuItem closeMenuItem;
    private JLabel playButtonAm;
    private JLabel playButtonBr;
    private volatile String selectedText;
    private volatile File[] selectedFiles = new File[2];
    private AtomicReference<Lang> srcLang = new AtomicReference<>(Lang.EN);
    private AtomicReference<Lang> trgLang = new AtomicReference<>(Lang.RU);
    private AtomicInteger amountViewWords = new AtomicInteger(10);
    private Set<List> historySet = Collections.synchronizedSet(new HashSet<>());
    @Autowired
    private TranslateService translateService;

    @Value(value = "${custom.prop.src.lang}")
    private String textSrcLang;

    @Value(value = "${custom.prop.trg.lang}")
    private String textTrgLang;

    @Value(value = "${custom.prop.amount_view_words}")
    private String textAmountViewWords;

    @Value(value = "${custom.prop.auto.play.am}")
    private volatile Boolean amAutoPlay;

    @Value(value = "${custom.prop.auto.play.br}")
    private volatile Boolean brAutoPlay;

    private String rootWorkspace;
    private LinkedBlockingQueue<Object> dispathBlockingQueue = new LinkedBlockingQueue<>();
    private String[] lastPlayWord = new String[Phonetic.values().length];

    @Autowired
    public PopupWindow(@Value(value = "${workspace}") String rootWorkspace) {
        this.rootWorkspace = rootWorkspace;
    }

    @org.springframework.context.event.EventListener({ContextClosedEvent.class})
    public void closeContext() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                popupFrame.dispose();
            }
        });
    }

    Thread dispathThread;

    private void init() throws ParseException, InterruptedException, IOException {
        if (!StringUtils.isBlank(textSrcLang)) {
            srcLang.set(Lang.valueOf(textSrcLang.toUpperCase()));
        }
        if (!StringUtils.isBlank(textTrgLang)) {
            trgLang.set(Lang.valueOf(textTrgLang.toUpperCase()));
        }
        if (!StringUtils.isBlank(textAmountViewWords)) {
            amountViewWords = new AtomicInteger(Integer.valueOf(textAmountViewWords));
        }

        menu = new JPopupMenu();
        closeMenuItem = new JMenuItem("close/copy");
        Font defaultFont = UIManager.getDefaults().getFont("TextPane.font").deriveFont(10);
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FOREGROUND, Color.GRAY);
        Font f = defaultFont.deriveFont(Font.ITALIC | Font.CENTER_BASELINE ^ Font.BOLD, 10);
        closeMenuItem.setFont(f.deriveFont(attributes));
        closeMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("client/images/close.png")));
        closeMenuItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ClipboardObserver.INSTANCE.copyTextToClipboard(
                            viewer.getDocument().getText(0, viewer.getDocument().getLength()));
                } catch (BadLocationException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        menu.add(closeMenuItem);
        JPanel panel = new JPanel();
        panel.add(createTextViewSection());
        menu.setFocusable(true);
        menu.add(panel);
        playPanel = new JPanel();
        playPanel.add(createPlayButtonSection(phonetic -> {
            File file = selectedFiles[phonetic.ordinal()];
            if (file != null) {
                AudioHelper.play(file.getAbsolutePath());
            }
        }));
        playPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        menu.add(playPanel);

        popupFrame = new JFrame();
        popupFrame.setUndecorated(true);
        popupFrame.setSize(0, 0);
        popupFrame.setType(Window.Type.POPUP);

        ClipboardObserver.INSTANCE.addActionListener((text, x, y, clickCount) -> {
            if (!dispathBlockingQueue.offer(new ClipboardEvent(text, x, y, clickCount))) {
                logger.info("The clipboard queue is overlow, it size is :" + dispathBlockingQueue.size());
            }
        });
        Application.PROPERTYSUPPORT.addPropertyChangeListener(this);
        popupFrame.setVisible(true);
        popupFrame.setExtendedState(JFrame.ICONIFIED);
        Thread thread = new Thread(() -> {
            ManagerWindow.launch(new String[0]);
        });
        thread.setDaemon(true);
        thread.start();
        dispathThread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    Object event = dispathBlockingQueue.take();
                    if (event instanceof ClipboardEvent) {
                        ClipboardEvent ce = (ClipboardEvent) event;
                        PopupWindow.this.processClipboardEvent(ce.text, ce.x, ce.y, ce.clickCount);
                    } else if (event instanceof TranslationEvent) {
                        TranslationEvent te = (TranslationEvent) event;
                        PopupWindow.this.doComplete(te.model, te.trgLang);
                    } else {
                        throw new IllegalArgumentException("dispathBlockingQueue contains unknow event");
                    }
                }
            } catch (InterruptedException | NoSuchMethodException | IOException | ScriptException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
        dispathThread.setDaemon(true);
        dispathThread.start();
        ClipboardObserver.INSTANCE.setActive(true);
    }

    private void processClipboardEvent(String text, int x, int y, int clickCount) throws IOException, NoSuchMethodException, ScriptException {
        String normalText = StringUtils.defaultString(Utils.normalText(text), "");
        normalText = normalText.substring(0, normalText.length() > 255 ? 255 : normalText.length());
        if ((x & y & clickCount) != -1) {
            currentScreenPoint.setLocation(x, y);
        }
        if (StringUtils.isBlank(normalText)) {
            hideMenu(x, y, clickCount, menu);
            return;
        }
        selectedText = normalText;
        Point fpoint = popupFrame.getLocationOnScreen();
        viewer.read(new StringReader(formtHtml(new DictionaryModel(Lang.EN, ""))), new HTMLDocument());
        playPanel.setVisible(false);
        if (clickCount == -1) {
            menu.show(popupFrame, currentScreenPoint.x - fpoint.x, currentScreenPoint.y - fpoint.y + 20);
        } else {
            menu.show(popupFrame, x - fpoint.x, y - fpoint.y + 20);
        }
        selectedFiles[0] = null;
        selectedFiles[1] = null;
        translateService.syncTranslate(selectedText, srcLang.get(), trgLang.get(), new TranslateService.Callback() {
            @Override
            public void onComplete(DictionaryModel model, Lang responseTrgLang) {
                if (!dispathBlockingQueue.offer(new TranslationEvent(model, responseTrgLang))) {
                    logger.info("The clipboard queue is overlow, it size is :" + dispathBlockingQueue.size());
                }
            }

            @Override
            public void onFailure(BaseException t) {
                logger.error(t.getMessage(), t);
            }
        });
    }

    public void doComplete(DictionaryModel model, Lang responseTrgLang) {
        if (selectedText.equals(model.getSource())
                && model.getSourceLang() == srcLang.get() && responseTrgLang == trgLang.get()) {
            try {
                addHistory(model, responseTrgLang);
                synchronized (PopupWindow.this) {
                    updateTranslateContext(model);
                    playWord();
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    private synchronized void addHistory(DictionaryModel model, Lang trgLang) {
        if (Utils.isWord(model.getSource())) {
            List key = Arrays.asList(model.getSource(), model.getSourceLang(), trgLang);
            if (!historySet.contains(key)) {
                historySet.add(key);
                Application.PROPERTYSUPPORT.firePropertyChange(Application.PropertySupport.Property.HISTORY, null, new AbstractMap.SimpleImmutableEntry(trgLang, model));
            }
        }
    }

    private void hideMenu(int x, int y, int clickCount, JComponent popup) {
        if (!popup.isVisible() || !popup.isShowing()) {
            return;
        }
        Point p2 = popup.getLocationOnScreen();
        Rectangle rectangle = SwingUtilities.computeIntersection(x, y, 1, 1, new Rectangle(p2.x, p2.y, popup.getWidth(), popup.getHeight()));
        if (clickCount == 1 && (rectangle.x | rectangle.y | rectangle.width | rectangle.height) == 0) {
            popup.setVisible(false);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        init();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Application.PropertySupport.Property.SRC_LANG.equalsEvent(evt)) {
            srcLang.set((Lang) evt.getNewValue());
        } else if (Application.PropertySupport.Property.TRG_LANG.equalsEvent(evt)) {
            trgLang.set((Lang) evt.getNewValue());
        } else if (Application.PropertySupport.Property.AMOUNT_VIEW_WORDS.equalsEvent(evt)) {
            amountViewWords.set((Integer) evt.getNewValue());
        } else if (Application.PropertySupport.Property.AM_AUTO_PLAY.equalsEvent(evt)) {
            amAutoPlay = (Boolean) evt.getNewValue();
        } else if (Application.PropertySupport.Property.BR_AUTO_PLAY.equalsEvent(evt)) {
            brAutoPlay = (Boolean) evt.getNewValue();
        }
    }

    private JComponent createTextViewSection() {
        viewer = new JEditorPane();
        viewer.setEditorKit(new HTMLEditorKitExt(rootWorkspace));
        //EditorKit kit = viewer.getEditorKitForContentType("text/html");
        viewer.setEditable(false);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.add(viewer);
        scrollPane.setViewportView(viewer);
        scrollPane.setBorder(BorderFactory.createLineBorder(viewer.getBackground(), 1));
        viewer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (menu.isVisible()) {
                    menu.setPopupSize(
                            viewer.getWidth() + 20,
                            viewer.getHeight() + playPanel.getVisibleRect().height + closeMenuItem.getPreferredSize().height + 20);
                    menu.repaint();
                }
            }
        });
        return scrollPane;
    }

    private JComponent createPlayButtonSection(PlayListener playListener) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 2, 10, 0));
        final ImageIcon imageIconAm1 = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("client/images/am1.png"));
        final ImageIcon imageIconAm2 = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("client/images/am2.png"));
        imageIconAm1.setImage(imageIconAm1.getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT));
        imageIconAm2.setImage(imageIconAm2.getImage().getScaledInstance(20, 15, Image.SCALE_DEFAULT));
        playButtonAm = new JLabel();
        playButtonAm.setIcon(imageIconAm2);
        playButtonAm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(playButtonAm);
        playButtonAm.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                playButtonAm.setIcon(imageIconAm1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                playButtonAm.setIcon(imageIconAm2);
            }
        });
        playButtonAm.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (playListener != null) {
                    playListener.actionPerformed(Phonetic.AM);
                }
            }
        });

        final ImageIcon imageIconBr1 = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("client/images/br1.png"));
        final ImageIcon imageIconBr2 = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("client/images/br2.png"));
        imageIconBr1.setImage(imageIconBr1.getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT));
        imageIconBr2.setImage(imageIconBr2.getImage().getScaledInstance(20, 15, Image.SCALE_DEFAULT));
        playButtonBr = new JLabel();
        playButtonBr.setIcon(imageIconBr2);
        playButtonBr.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(playButtonBr);
        playButtonBr.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                playButtonBr.setIcon(imageIconBr1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                playButtonBr.setIcon(imageIconBr2);
            }
        });
        playButtonBr.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (playListener != null) {
                    playListener.actionPerformed(Phonetic.AM.BR);
                }
            }
        });
        return panel;
    }

    private boolean canPlay(DictionaryModel model, Phonetic phonetic) {
        if (Utils.isWord(selectedText)) {
            selectedFiles[phonetic.ordinal()] = translateService.getAudioFile(model, phonetic);
            return selectedFiles[phonetic.ordinal()] != null;
        } else {
            return false;
        }
    }

    private void updateTranslateContext(DictionaryModel model) throws IOException {
        playPanel.setVisible(Utils.isWord(selectedText));
        playButtonAm.setEnabled(canPlay(model, Phonetic.AM));
        playButtonBr.setEnabled(canPlay(model, Phonetic.BR));
        StringReader stringReader = new StringReader(formtHtml(model));
        HTMLDocument htmlDocument = new HTMLDocument();
        if (!playPanel.isVisible()) menu.setVisible(false);
        viewer.read(stringReader, htmlDocument);
        if (!playPanel.isVisible()) menu.setVisible(true);
    }

    private synchronized void playWord() throws InterruptedException {
        boolean isPlayed = false;
        String lastWord = StringUtils.defaultIfBlank(selectedText, "");
        for (Phonetic phonetic : Phonetic.values()) {
            File f = selectedFiles[phonetic.ordinal()];
            if (f == null || lastWord.equals(lastPlayWord[phonetic.ordinal()])) {
                continue;
            }
            lastPlayWord[phonetic.ordinal()] = lastWord;
            if (Phonetic.AM == phonetic && amAutoPlay) {
                if (isPlayed) {
                    Thread.sleep(500);
                }
                AudioHelper.play(f.getAbsolutePath());
                isPlayed = true;
            } else if (Phonetic.BR == phonetic && brAutoPlay) {
                if (isPlayed) {
                    Thread.sleep(500);
                }
                AudioHelper.play(f.getAbsolutePath());
                isPlayed = true;
            }
        }
    }

    private String formtHtml(DictionaryModel model) throws BaseException {
        try {
            String trn = model.toJson(EnumSet.of(trgLang.get()), DictionaryModel.Fields.TRANSLATIONS).toString();
            //System.out.println(model.toJson().toString());
            Font defaultFont = UIManager.getDefaults().getFont("TextPane.font").deriveFont(13);
            FontMetrics fm = viewer.getFontMetrics(defaultFont);
            double koef = 2.0 / 3.0;
            double h = Math.sqrt(fm.stringWidth(trn) * fm.getHeight() * koef);
            double w = h / koef;
            Dimension wsz = Toolkit.getDefaultToolkit().getScreenSize();
            if (w > wsz.getWidth()) {
                w = wsz.getWidth() * 0.5;
            }
            if (h > wsz.getHeight()) {
                h = wsz.getHeight() * 0.5;
            }
            String s = JsonTransformer.createJsonTransformer()
                    .setMaxCount(amountViewWords.get())
                    .setWidth((int) w)
                    .setHeight((int) h)
                    .setFontFamily(defaultFont.getFamily())
                    .convertJsonToHtml(model.toJson(EnumSet.of(trgLang.get()),
                            DictionaryModel.Fields.SOURCE,
                            DictionaryModel.Fields.LANG,
                            DictionaryModel.Fields.TRANSCRIPTIONS,
                            DictionaryModel.Fields.TRANSLATIONS).toString(), Utils.isWord(selectedText)
                            ? JsonTransformer.XSL.HTML : JsonTransformer.XSL.WORDS_HTML);
            //System.out.println(s);
            return s;
        } catch (BaseException ex) {
            ex.printStackTrace();
            throw new BaseException(ex);
        }
    }

    interface PlayListener {
        void actionPerformed(Phonetic phonetic);
    }

    static class ClipboardEvent {
        private final String text;
        private final int x;
        private final int y;
        private final int clickCount;

        public ClipboardEvent(String text, int x, int y, int clickCount) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.clickCount = clickCount;
        }
    }

    static class TranslationEvent {
        private final DictionaryModel model;
        private final Lang trgLang;

        public TranslationEvent(DictionaryModel model, Lang trgLang) {
            this.model = model;
            this.trgLang = trgLang;
        }
    }

    static class HTMLEditorKitExt extends HTMLEditorKit {
        private String rootWorkspace;
        private final ViewFactory defaultFactory = new HTMLFactory() {
            public View create(Element elem) {
                View view = super.create(elem);
                if (view instanceof ImageView) {
                    try {
                        return loadImage(ImageView.class.cast(view));
                    } catch (IOException | URISyntaxException ex) {
                        throw new BaseException(ex);
                    }
                }
                return view;
            }
        };

        public HTMLEditorKitExt(String rootWorkspace) {
            this.rootWorkspace = rootWorkspace;
        }

        public ViewFactory getViewFactory() {
            return defaultFactory;
        }

        private ImageView loadImage(ImageView view) throws IOException, URISyntaxException {
            URL src = view.getImageURL();
            if (src != null && Files.exists(Paths.get(src.toURI()), LinkOption.NOFOLLOW_LINKS)) {
                return view;
            }
            logger.debug("imageURL: " + src == null ? "" : src.getFile());
            String imgResName = src.getFile().substring(1);
            src = Thread.currentThread().getContextClassLoader().getResource(imgResName);
            if (src == null) {
                throw new BaseException("not found image resource: " + imgResName);
            }
            Image image;
            Dictionary cache = new Hashtable<>();
            //get info from private method
            view.getDocument().putProperty("imageCache", cache);
            ImageIcon imageIcon = new ImageIcon(src);
            cache.put(view.getImageURL(), imageIcon.getImage());
            try {
                image = view.getImage();
            } catch (Exception ex) {
                image = null;
            }
            if (image != null) {
                return view;
            }
            final Path imageFilePath = Paths.get(rootWorkspace, imgResName);
            File dir = imageFilePath.toFile().getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (!Files.exists(imageFilePath, LinkOption.NOFOLLOW_LINKS)) {
                try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(imgResName)) {
                    Files.copy(in, imageFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            view.getDocument().putProperty("imageCache", null);
            return new ImageView(view.getElement()) {
                public URL getImageURL() {
                    try {
                        return imageFilePath.toUri().toURL();
                    } catch (MalformedURLException ex) {
                        throw new BaseException(ex);
                    }
                }
            };
        }
    }
}