package com.gtranslator.client;

import com.gtranslator.Application;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;

import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.LogManager;

/*
 * grant { permission java.awt.AWTPermission "accessClipboard" };
 */
public class ClipboardObserver implements PropertyChangeListener {
    public static final ClipboardObserver INSTANCE = new ClipboardObserver();
    private static final Logger logger = Logger.getLogger(ClipboardObserver.class);
    private AtomicBoolean isActive = new AtomicBoolean(false);
    private AtomicReference<MODE> mode = new AtomicReference<>(MODE.COPY);
    private List<ActionListener> actionListeners = new ArrayList<>();
    private Clipboard copyClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private Clipboard selClipboard = Toolkit.getDefaultToolkit().getSystemSelection();
    private String clipboardText;

    private ClipboardObserver() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            logger.error(ex.getMessage(), ex);
            System.exit(1);
        }
        GlobalScreen.addNativeMouseListener(new NativeMouseListenerExt());
        GlobalScreen.addNativeKeyListener(new NativeKeyListenerExt());

        LogManager.getLogManager().reset();
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        Application.PROPERTYSUPPORT.addPropertyChangeListener(this);
        Clipboard clipboard = mode.get() == MODE.SELECT ? selClipboard : copyClipboard;
        Transferable clipData = clipboard.getContents(null);
        if (clipData != null && clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                clipboardText = ObjectUtils.defaultIfNull(clipData.getTransferData(DataFlavor.stringFlavor), "").toString();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public void setActive(boolean b) {
        isActive.set(b);
    }

    public void setMode(MODE mode) {
        this.mode.set(mode);
    }

    public void addActionListener(ActionListener actionListener) {
        synchronized (actionListeners) {
            actionListeners.add(actionListener);
        }
    }

    public void copyTextToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        copyClipboard.setContents(stringSelection, stringSelection);
    }

    private void fireEventOnClipboardChanged(int x, int y, int clickCount) throws IOException, UnsupportedFlavorException, NoSuchMethodException, ScriptException {
        if (!isActive.get()) {
            return;
        }
        synchronized (actionListeners) {
            try {
                Clipboard clipboard = mode.get() == MODE.SELECT ? selClipboard : copyClipboard;
                Transferable clipData = clipboard.getContents(null);
                if (clipData != null && clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    Object text = clipData.getTransferData(DataFlavor.stringFlavor);
                    if (text != null && !text.toString().equals(clipboardText)) {
                        clipboardText = text.toString();
                        for (ActionListener actionListener : actionListeners) {
                            actionListener.textChanged(clipboardText, x, y, clickCount);
                        }
                        return;
                    }
                }
                for (ActionListener actionListener : actionListeners) {
                    actionListener.textChanged(null, x, y, clickCount);
                }
            } catch (Exception ex) {
                for (ActionListener actionListener : actionListeners) {
                    actionListener.textChanged(null, x, y, clickCount);
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Application.PropertySupport.Property.ACTIVE.equalsEvent(evt)) {
            isActive.set((Boolean) evt.getNewValue());
        } else if (Application.PropertySupport.Property.MODE.equalsEvent(evt)) {
            mode.set(MODE.valueOf(evt.getNewValue().toString().toUpperCase()));
        }
    }

    public void close() {
        try {
            setActive(false);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }

    public enum MODE {
        SELECT, COPY
    }

    public interface ActionListener {
        void textChanged(String text, int x, int y, int clickCount) throws IOException, NoSuchMethodException, ScriptException;
    }

    private static class NativeKeyListenerExt implements NativeKeyListener {

        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            if (e.getModifiers() == 2
                    && e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            } else if (e.getKeyCode() == NativeKeyEvent.VC_META_L) {
            }
        }

        @Override
        public void nativeKeyReleased(NativeKeyEvent e) {
            if (ClipboardObserver.INSTANCE.mode.get() == MODE.COPY) {
                boolean ctrl = (e.getModifiers() & 34) != 0;
                boolean c = e.getKeyCode() == 46;
                if (ctrl && c) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            ClipboardObserver.INSTANCE.fireEventOnClipboardChanged(-1, -1, -1);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        } catch (UnsupportedFlavorException | NoSuchMethodException | ScriptException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    });
                }
            }
        }

        @Override
        public void nativeKeyTyped(NativeKeyEvent e) {
        }

    }

    private static class NativeMouseListenerExt implements NativeMouseListener {

        @Override
        public void nativeMouseClicked(NativeMouseEvent e) {
        }

        @Override
        public void nativeMousePressed(NativeMouseEvent e) {
        }

        @Override
        public void nativeMouseReleased(NativeMouseEvent e) {
            SwingUtilities.invokeLater(() -> {
                try {
                    ClipboardObserver.INSTANCE.fireEventOnClipboardChanged(e.getX(), e.getY(), e.getClickCount());
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (UnsupportedFlavorException | NoSuchMethodException | ScriptException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            });
        }
    }

}
