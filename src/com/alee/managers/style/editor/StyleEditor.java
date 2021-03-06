/*
 * This file is part of WebLookAndFeel library.
 *
 * WebLookAndFeel library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebLookAndFeel library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebLookAndFeel library.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alee.managers.style.editor;

import com.alee.extended.layout.VerticalFlowLayout;
import com.alee.extended.panel.BorderPanel;
import com.alee.extended.panel.CenterPanel;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.WebButtonGroup;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.extended.window.PopOverDirection;
import com.alee.extended.window.WebPopOver;
import com.alee.laf.StyleConstants;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.colorchooser.WebColorChooserPanel;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.panel.WebPanelUI;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollBar;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.scroll.WebScrollPaneUI;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.tabbedpane.TabbedPaneStyle;
import com.alee.laf.tabbedpane.WebTabbedPane;
import com.alee.laf.text.WebTextField;
import com.alee.laf.toolbar.ToolbarStyle;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.managers.glasspane.GlassPaneManager;
import com.alee.managers.glasspane.WebGlassPane;
import com.alee.managers.hotkey.Hotkey;
import com.alee.managers.hotkey.HotkeyManager;
import com.alee.managers.hotkey.HotkeyRunnable;
import com.alee.managers.style.StyleManager;
import com.alee.managers.style.SupportedComponent;
import com.alee.managers.style.data.SkinInfo;
import com.alee.managers.style.data.SkinInfoConverter;
import com.alee.managers.style.skin.CustomSkin;
import com.alee.managers.tooltip.TooltipManager;
import com.alee.utils.*;
import com.alee.utils.swing.DocumentChangeListener;
import com.alee.utils.swing.IntTextDocument;
import com.alee.utils.swing.WebTimer;
import com.alee.utils.xml.ColorConverter;
import com.alee.utils.xml.ResourceFile;
import com.alee.utils.xml.ResourceLocation;
import com.thoughtworks.xstream.converters.ConversionException;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.RUndoManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mikle Garin
 */

public class StyleEditor extends WebFrame
{
    /**
     * todo 1. Translate editor
     */

    private static final ImageIcon info = new ImageIcon ( StyleEditor.class.getResource ( "icons/status/info.png" ) );
    private static final ImageIcon ok = new ImageIcon ( StyleEditor.class.getResource ( "icons/status/ok.png" ) );
    private static final ImageIcon error = new ImageIcon ( StyleEditor.class.getResource ( "icons/status/error.png" ) );

    private static final ImageIcon tabIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/tab.png" ) );

    private static final BufferedImage magnifier =
            ImageUtils.getBufferedImage ( new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/magnifierImage.png" ) ) );

    private WebToolBar toolBar;
    private WebPanel container;
    private WebSplitPane split;
    private WebPanel componentViewer;
    private WebPanel editorsContainer;
    private WebStatusBar statusbar;

    private WebLabel statusMessage;

    private final List<JComponent> previewComponents = new ArrayList<JComponent> ();
    private final List<WebPanel> boundsPanels = new ArrayList<WebPanel> ();

    private int updateDelay = 50;
    private int zoomFactor = 4;
    private ComponentOrientation orientation = WebLookAndFeel.getOrientation ();
    private boolean enabled = true;

    private final ResourceFile baseSkinFile;
    private List<RSyntaxTextArea> editors;

    public StyleEditor ()
    {
        super ( "WebLaF skin editor" );
        setIconImages ( WebLookAndFeel.getImages () );

        // todo Make changeable through constructor
        baseSkinFile = new ResourceFile ( ResourceLocation.nearClass, "resources/StyleEditorSkin.xml", StyleEditorSkin.class );

        initializeContainer ();
        initializeToolBar ();
        initializeStatusBar ();
        initializeViewer ();
        initializeEditors ();

        setDefaultCloseOperation ( WindowConstants.EXIT_ON_CLOSE );
        setSize ( 1000, 700 );
        setLocationRelativeTo ( null );
    }

    private void initializeToolBar ()
    {
        toolBar = new WebToolBar ( WebToolBar.HORIZONTAL );
        toolBar.setToolbarStyle ( ToolbarStyle.attached );
        toolBar.setMargin ( 4 );
        toolBar.setSpacing ( 4 );
        toolBar.setFloatable ( false );

        final ImageIcon magnifierIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/magnifier.png" ) );
        final WebToggleButton magnifierButton = new WebToggleButton ( "Magnifier", magnifierIcon );
        TooltipManager.setTooltip ( magnifierButton, magnifierIcon, "Show/hide magnifier tool" );
        magnifierButton.addHotkey ( Hotkey.ALT_Q );
        magnifierButton.setRound ( 0 );
        magnifierButton.setFocusable ( false );
        initializeMagnifier ( magnifierButton );
        final WebButton zoomFactorButton = new WebButton ( "4x" );
        zoomFactorButton.setRound ( 0 );
        zoomFactorButton.setFocusable ( false );
        zoomFactorButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                final WebPopupMenu menu = new WebPopupMenu ();
                for ( int i = 2; i <= 6; i++ )
                {
                    final int factor = i;
                    final JMenuItem menuItem = new WebMenuItem ( i + "x zoom" );
                    menuItem.addActionListener ( new ActionListener ()
                    {
                        @Override
                        public void actionPerformed ( final ActionEvent e )
                        {
                            zoomFactor = factor;
                            zoomFactorButton.setText ( factor + "x" );
                        }
                    } );
                    menu.add ( menuItem );
                }
                menu.showBelowMiddle ( zoomFactorButton );
            }
        } );
        toolBar.add ( new WebButtonGroup ( magnifierButton, zoomFactorButton ) );

        final ImageIcon boundsIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/bounds.png" ) );
        final WebToggleButton boundsButton = new WebToggleButton ( "Bounds", boundsIcon );
        TooltipManager.setTooltip ( boundsButton, boundsIcon, "Show/hide component bounds" );
        boundsButton.addHotkey ( Hotkey.ALT_W );
        boundsButton.setRound ( 0 );
        boundsButton.setFocusable ( false );
        boundsButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                for ( final WebPanel boundsPanel : boundsPanels )
                {
                    boundsPanel.setStyleId ( boundsButton.isSelected () ? "dashed-border" : "empty-border" );
                }
            }
        } );
        toolBar.add ( boundsButton );

        final ImageIcon disabledIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/disabled.png" ) );
        final WebToggleButton disabledButton = new WebToggleButton ( "Disabled", disabledIcon );
        TooltipManager.setTooltip ( disabledButton, disabledIcon, "Disable/enable components" );
        disabledButton.addHotkey ( Hotkey.ALT_D );
        disabledButton.setRound ( 0 );
        disabledButton.setFocusable ( false );
        disabledButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                enabled = !enabled;

                // Applying enabled state to separate components as they might not be visible on panel
                for ( final JComponent component : previewComponents )
                {
                    SwingUtils.setEnabledRecursively ( component, enabled );
                }
            }
        } );
        toolBar.add ( disabledButton );

        final ImageIcon orientationIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/orientation.png" ) );
        final WebToggleButton orientationButton = new WebToggleButton ( "RTL orientation", orientationIcon );
        TooltipManager.setTooltip ( orientationButton, orientationIcon, "Change components orientation" );
        orientationButton.addHotkey ( Hotkey.ALT_R );
        orientationButton.setRound ( 0 );
        orientationButton.setFocusable ( false );
        orientationButton.setSelected ( !orientation.isLeftToRight () );
        orientationButton.addActionListener ( new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                orientation = orientation.isLeftToRight () ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;

                // Applying orientation to whole panel first
                componentViewer.applyComponentOrientation ( orientation );

                // Applying orientation to separate components as they might not be visible on panel
                for ( final JComponent component : previewComponents )
                {
                    component.applyComponentOrientation ( orientation );
                }
            }
        } );
        toolBar.add ( orientationButton );

        //

        final ImageIcon updateIcon = new ImageIcon ( StyleEditor.class.getResource ( "icons/editor/update.png" ) );
        final WebLabel delayLabel = new WebLabel ( "Skin update delay:", updateIcon ).setMargin ( 4 );
        final WebTextField delayField = new WebTextField ( new IntTextDocument (), "" + updateDelay, 3 );
        delayField.setHorizontalAlignment ( WebTextField.CENTER );
        delayField.getDocument ().addDocumentListener ( new DocumentChangeListener ()
        {
            @Override
            public void documentChanged ( final DocumentEvent e )
            {
                try
                {
                    updateDelay = Integer.parseInt ( delayField.getText () );
                    if ( updateDelay < 0 )
                    {
                        updateDelay = 0;
                    }
                }
                catch ( final Throwable ex )
                {
                    // Ignore exceptions
                }
            }
        } );
        final WebLabel msLabel = new WebLabel ( "ms" ).setMargin ( 4 );
        toolBar.addToEnd ( new GroupPanel ( 4, delayLabel, delayField, msLabel ) );

        container.add ( toolBar, BorderLayout.NORTH );
    }

    private void initializeContainer ()
    {
        container = new WebPanel ();
        getContentPane ().add ( container, BorderLayout.CENTER );

        split = new WebSplitPane ( WebSplitPane.HORIZONTAL_SPLIT, true );
        split.setDividerLocation ( 300 );
        container.add ( new BorderPanel ( split, 7 ), BorderLayout.CENTER );
    }

    private void initializeStatusBar ()
    {
        statusbar = new WebStatusBar ();

        statusMessage = new WebLabel ( "Edit XML at the right side and see UI changes at the left side!", info ).setMargin ( 4 );
        statusbar.add ( statusMessage );
        statusbar.addToEnd ( new WebMemoryBar ().setPreferredWidth ( 200 ) );

        container.add ( statusbar, BorderLayout.SOUTH );
    }

    private void initializeViewer ()
    {
        componentViewer = new WebPanel ( new VerticalFlowLayout ( VerticalFlowLayout.TOP, 0, 15, true, false ) );
        componentViewer.setMargin ( 5 );
        final WebScrollPane previewScroll = new WebScrollPane ( componentViewer, false, false );
        previewScroll.setScrollBarStyleId ( "preview-scroll" );
        split.setLeftComponent ( previewScroll );

        //

        final WebScrollBar hsb = new WebScrollBar ( WebScrollBar.HORIZONTAL, 45, 10, 0, 100 );
        addViewComponent ( "JScrollBar (horizontal)", hsb, hsb, false );

        //

        final WebScrollBar vsb = new WebScrollBar ( WebScrollBar.VERTICAL, 45, 10, 0, 100 ).setPreferredHeight ( 100 );
        addViewComponent ( "JScrollBar (vertical)", vsb, vsb, true );

        //

        final WebLabel scrollComponent = new WebLabel ();
        scrollComponent.setPreferredSize ( new Dimension ( 1000, 600 ) );
        scrollComponent.setFocusable ( true );
        scrollComponent.addMouseListener ( new MouseAdapter ()
        {
            @Override
            public void mousePressed ( final MouseEvent e )
            {
                scrollComponent.requestFocusInWindow ();
            }
        } );

        final WebScrollPane sp = new WebScrollPane ( scrollComponent );
        sp.setPreferredSize ( new Dimension ( 1, 100 ) );
        addViewComponent ( "JScrollBar in JScrollPane", sp, sp, false );

        //

        final String[] comboData =
                new String[]{ "Mikle Garin", "Lilly Stewart", "Alex Jackson", "Joshua Martin", "Mark Einsberg", "Joe Phillips",
                        "Alice Manson", "Nancy Drew", "John Linderman", "Trisha Mathew" };
        final WebComboBox cb = new WebComboBox ( comboData );
        addViewComponent ( "JScrollBar in JComboBox", cb, cb, true );

        //

        final WebPopupMenu popupMenu = new WebPopupMenu ();
        popupMenu.add ( new WebMenuItem ( "Item 1", WebLookAndFeel.getIcon ( 16 ) ) );
        popupMenu.add ( new WebMenuItem ( "Item 2" ) );
        popupMenu.add ( new WebMenuItem ( "Item 3" ) );
        popupMenu.addSeparator ();
        popupMenu.add ( new WebMenuItem ( "Item 4", WebLookAndFeel.getIcon ( 16 ), Hotkey.ALT_F4 ) );

        final WebButton popupButton = new WebButton ( "Show popup menu", new ActionListener ()
        {
            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                popupMenu.showBelowMiddle ( ( WebButton ) e.getSource () );
            }
        } );

        addViewComponent ( "JPopupMenu under JButton", popupButton, popupMenu, true );
    }

    private void addViewComponent ( final String title, final JComponent displayedView, final JComponent view, final boolean center )
    {
        final SupportedComponent type = SupportedComponent.getComponentTypeByUIClassID ( view.getUIClassID () );

        final WebLabel titleLabel = new WebLabel ( title, type.getIcon () ).setMargin ( 0, 7, 3, 0 );

        final WebPanel boundsPanel = new WebPanel ( displayedView );
        boundsPanel.setStyleId ( "empty-border" );
        boundsPanels.add ( boundsPanel );

        final WebPanel viewPanel = new WebPanel ( center ? new CenterPanel ( boundsPanel ) : boundsPanel );
        viewPanel.setStyleId ( "inner-shade" );

        final WebPanel container = new WebPanel ( new BorderLayout ( 0, 0 ) );
        container.add ( titleLabel, BorderLayout.NORTH );
        container.add ( viewPanel, BorderLayout.CENTER );
        componentViewer.add ( container );

        titleLabel.addMouseListener ( new MouseAdapter ()
        {
            @Override
            public void mousePressed ( final MouseEvent e )
            {
                viewPanel.setVisible ( !viewPanel.isVisible () );
                componentViewer.revalidate ();
                componentViewer.repaint ();
            }
        } );

        previewComponents.add ( view );
    }

    private void initializeEditors ()
    {
        // Creating XML editors tabbed pane
        final WebTabbedPane editorTabs = new WebTabbedPane ( TabbedPaneStyle.attached );
        editorsContainer = new WebPanel ( true, editorTabs );

        // Loading editor code theme
        final Theme theme = loadXmlEditorTheme ();

        // Parsing all related files
        final List<String> xmlContent = new ArrayList<String> ();
        final List<String> xmlNames = new ArrayList<String> ();
        final List<ResourceFile> xmlFiles = new ArrayList<ResourceFile> ();
        loadSkinSources ( xmlContent, xmlNames, xmlFiles );

        // Creating editor tabs
        editors = new ArrayList<RSyntaxTextArea> ( xmlContent.size () );
        for ( int i = 0; i < xmlContent.size (); i++ )
        {
            final WebPanel tabContent = new WebPanel ();
            tabContent.add ( new TabContentSeparator (), BorderLayout.NORTH );
            tabContent.add ( createSingleXmlEditor ( theme, xmlContent.get ( i ), xmlFiles.get ( i ) ), BorderLayout.CENTER );
            editorTabs.addTab ( xmlNames.get ( i ), tabContent );
            editorTabs.setIconAt ( i, tabIcon );
        }

        // Adding XML editors container into split
        split.setRightComponent ( editorsContainer );
    }

    private RTextScrollPane createSingleXmlEditor ( final Theme theme, final String xml, final ResourceFile xmlFile )
    {
        final RUndoManager[] xmlEditorHistory = new RUndoManager[ 1 ];
        final RSyntaxTextArea xmlEditor = new RSyntaxTextArea ()
        {
            @Override
            protected RUndoManager createUndoManager ()
            {
                xmlEditorHistory[ 0 ] = super.createUndoManager ();
                xmlEditorHistory[ 0 ].setLimit ( 50 );
                return xmlEditorHistory[ 0 ];
            }
        };
        xmlEditor.setSyntaxEditingStyle ( SyntaxConstants.SYNTAX_STYLE_XML );
        xmlEditor.setMargin ( new Insets ( 0, 5, 0, 0 ) );
        xmlEditor.setAntiAliasingEnabled ( true );
        xmlEditor.setUseFocusableTips ( true );
        xmlEditor.setTabSize ( 4 );
        xmlEditor.setCodeFoldingEnabled ( true );
        xmlEditor.setPaintTabLines ( false );
        xmlEditor.setWhitespaceVisible ( false );
        xmlEditor.setEOLMarkersVisible ( false );

        xmlEditor.setText ( xml );
        xmlEditor.setCaretPosition ( 0 );

        xmlEditor.setHyperlinksEnabled ( true );
        xmlEditor.setLinkGenerator ( new CodeLinkGenerator () );

        HotkeyManager.registerHotkey ( xmlEditor, xmlEditor, Hotkey.CTRL_SHIFT_Z, new HotkeyRunnable ()
        {
            @Override
            public void run ( final KeyEvent e )
            {
                xmlEditor.undoLastAction ();
            }
        } );

        // Creating editor scroll with preferred settings
        final RTextScrollPane xmlEditorScroll = new RTextScrollPane ( xmlEditor );
        xmlEditorScroll.setVerticalScrollBarPolicy ( WebScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        ( ( WebScrollPaneUI ) xmlEditorScroll.getUI () ).setDrawBorder ( false );
        ( ( WebPanelUI ) xmlEditorScroll.getGutter ().getUI () ).setStyleId ( "editor-gutter" );

        // Applying editor theme after scroll creation
        theme.apply ( xmlEditor );

        // Cleaning initial history
        xmlEditorHistory[ 0 ].discardAllEdits ();

        // Start listening edits
        xmlEditor.getDocument ().addDocumentListener ( new DocumentChangeListener ()
        {
            private final WebTimer updateTimer = new WebTimer ( updateDelay, new ActionListener ()
            {
                @Override
                public void actionPerformed ( final ActionEvent e )
                {
                    SkinInfoConverter.addCustomResource ( xmlFile.getClassName (), xmlFile.getSource (), xmlEditor.getText () );
                    applySkin ();
                }
            } ).setRepeats ( false );

            @Override
            public void documentChanged ( final DocumentEvent e )
            {
                updateTimer.restart ( updateDelay );
            }
        } );

        editors.add ( xmlEditor );
        return xmlEditorScroll;
    }

    private void loadSkinSources ( final List<String> xmlContent, final List<String> xmlNames, final List<ResourceFile> xmlFiles )
    {
        // Adding base skin file
        final List<ResourceFile> resources = new ArrayList<ResourceFile> ();
        resources.add ( baseSkinFile );

        // Parsing all related skin files
        while ( resources.size () > 0 )
        {
            try
            {
                loadFirstResource ( resources, xmlContent, xmlNames, xmlFiles );
            }
            catch ( final IOException e )
            {
                e.printStackTrace ();
            }
        }
    }

    private void loadFirstResource ( final List<ResourceFile> resources, final List<String> xmlContent, final List<String> xmlNames,
                                     final List<ResourceFile> xmlFiles ) throws IOException
    {
        final ResourceFile rf = resources.get ( 0 );
        final Source xmlSource = new Source ( ReflectUtils.getClassSafely ( rf.getClassName () ).getResource ( rf.getSource () ) );
        xmlSource.fullSequentialParse ();

        final Element baseClassTag = xmlSource.getFirstElement ( SkinInfoConverter.CLASS_NODE );
        final String baseClass = baseClassTag != null ? baseClassTag.getContent ().toString () : null;

        for ( final Element includeTag : xmlSource.getAllElements ( SkinInfoConverter.INCLUDE_NODE ) )
        {
            final String includeClass = includeTag.getAttributeValue ( SkinInfoConverter.NEAR_CLASS_ATTRIBUTE );
            final String finalClass = includeClass != null ? includeClass : baseClass;
            final String src = includeTag.getContent ().toString ();
            resources.add ( new ResourceFile ( ResourceLocation.nearClass, src, finalClass ) );
        }

        xmlContent.add ( xmlSource.toString () );
        xmlNames.add ( new File ( rf.getSource () ).getName () );
        xmlFiles.add ( rf );

        resources.remove ( 0 );
    }

    private Theme loadXmlEditorTheme ()
    {
        try
        {
            return Theme.load ( StyleEditor.class.getResourceAsStream ( "resources/editorTheme.xml" ) );
        }
        catch ( final IOException e )
        {
            e.printStackTrace ();
            return null;
        }
    }

    private void applySkin ()
    {
        try
        {
            long time = System.currentTimeMillis ();
            StyleManager.applySkin ( new CustomSkin ( ( SkinInfo ) XmlUtils.fromXML ( editors.get ( 0 ).getText () ) ) );
            componentViewer.revalidate ();

            // Information in status bar
            time = System.currentTimeMillis () - time;
            statusMessage.setIcon ( ok );
            statusMessage.setText ( "Style updated succesfully within " + time + " ms" );
        }
        catch ( final ConversionException ex )
        {
            // Short stack trace for parse exceptions
            System.err.println ( "Unable to update skin: " + ex.getMessage () );
            ex.printStackTrace ();

            // Information in status bar
            statusMessage.setIcon ( error );
            statusMessage.setText ( "Fix syntax problems within the XML to update styling" );
        }
        catch ( final Throwable ex )
        {
            // Full stack trace for unknown exceptions
            System.err.println ( "Unable to update skin: " + ex.getMessage () );
            ex.printStackTrace ();

            // Information in status bar
            statusMessage.setIcon ( error );
            statusMessage.setText ( "Fix syntax problems within the XML to update styling" );
        }
    }

    /**
     * Initializes magnifier display action for the specified button.
     *
     * @param button magnifier display button
     */
    private void initializeMagnifier ( final WebToggleButton button )
    {
        final WebGlassPane glassPane = GlassPaneManager.getGlassPane ( StyleEditor.this );
        final JComponent zoomProvider = SwingUtils.getRootPane ( StyleEditor.this ).getLayeredPane ();
        button.addActionListener ( new ActionListener ()
        {
            private boolean visible = false;
            private AWTEventListener listener;
            private WebTimer forceUpdater;

            @Override
            public void actionPerformed ( final ActionEvent e )
            {
                performAction ();
            }

            protected void performAction ()
            {
                if ( !visible )
                {
                    visible = true;

                    if ( forceUpdater == null || listener == null )
                    {
                        forceUpdater = new WebTimer ( 200, new ActionListener ()
                        {
                            @Override
                            public void actionPerformed ( final ActionEvent e )
                            {
                                updateMagnifier ();
                            }
                        } );
                        listener = new AWTEventListener ()
                        {
                            @Override
                            public void eventDispatched ( final AWTEvent event )
                            {
                                SwingUtilities.invokeLater ( new Runnable ()
                                {
                                    @Override
                                    public void run ()
                                    {
                                        if ( visible )
                                        {
                                            forceUpdater.restart ();
                                            updateMagnifier ();
                                        }
                                    }
                                } );
                            }
                        };
                    }
                    Toolkit.getDefaultToolkit ().addAWTEventListener ( listener, AWTEvent.MOUSE_MOTION_EVENT_MASK );
                    Toolkit.getDefaultToolkit ().addAWTEventListener ( listener, AWTEvent.MOUSE_WHEEL_EVENT_MASK );
                    Toolkit.getDefaultToolkit ().addAWTEventListener ( listener, AWTEvent.MOUSE_EVENT_MASK );
                    updateMagnifier ();

                    setCursor ( SystemUtils.getTransparentCursor () );
                }
                else
                {
                    visible = false;

                    Toolkit.getDefaultToolkit ().removeAWTEventListener ( listener );
                    forceUpdater.stop ();
                    hideMagnifier ();

                    setCursor ( Cursor.getDefaultCursor () );
                }
            }

            protected void updateMagnifier ()
            {
                final Point mp = MouseInfo.getPointerInfo ().getLocation ();
                final Rectangle gb = SwingUtils.getBoundsOnScreen ( glassPane );
                if ( gb.contains ( mp ) )
                {
                    final Point gp = gb.getLocation ();
                    final int mx = mp.x - gp.x - magnifier.getWidth () / 2;
                    final int my = mp.y - gp.y - magnifier.getHeight () / 2;

                    final int w = 162 / zoomFactor;
                    final BufferedImage image = ImageUtils.createCompatibleImage ( w, w, Transparency.TRANSLUCENT );
                    final Graphics2D g2d = image.createGraphics ();
                    g2d.translate ( -( mp.x - gp.x - w / 2 ), -( mp.y - gp.y - w / 2 ) );
                    zoomProvider.paintAll ( g2d );
                    g2d.dispose ();

                    final BufferedImage finalImage = ImageUtils.createCompatibleImage ( 220, 220, Transparency.TRANSLUCENT );
                    final Graphics2D g = finalImage.createGraphics ();
                    g.setClip ( new Ellipse2D.Double ( 29, 29, 162, 162 ) );
                    g.drawImage ( image, 29, 29, 162, 162, null );
                    g.setClip ( null );
                    g.drawImage ( magnifier, 0, 0, null );
                    g.dispose ();

                    glassPane.setPaintedImage ( finalImage, new Point ( mx, my ) );
                }
                else
                {
                    hideMagnifier ();
                }
            }

            protected void hideMagnifier ()
            {
                glassPane.setPaintedImage ( null, null );
            }
        } );
    }

    /**
     * Custom tab content separator.
     */
    private class TabContentSeparator extends JComponent
    {
        @Override
        protected void paintComponent ( final Graphics g )
        {
            g.setColor ( new Color ( 237, 237, 237 ) );
            g.fillRect ( 0, 0, getWidth (), getHeight () - 1 );
            g.setColor ( StyleConstants.darkBorderColor );
            g.drawLine ( 0, getHeight () - 1, getWidth () - 1, getHeight () - 1 );
        }

        @Override
        public Dimension getPreferredSize ()
        {
            return new Dimension ( 0, 4 );
        }
    }

    /**
     * Code links generator for XML editor.
     */
    private class CodeLinkGenerator implements LinkGenerator
    {
        private final String contentStartTag = ">";
        private final String contentEndTag = "<";

        private final String trueString = "true";
        private final String falseString = "false";

        private final ColorConverter colorConverter = new ColorConverter ();

        public CodeLinkGenerator ()
        {
            super ();
        }

        @Override
        public LinkGeneratorResult isLinkAtOffset ( final RSyntaxTextArea source, final int pos )
        {
            final String code = source.getText ();
            final int wordStart = getContentStart ( code, pos );
            final int wordEnd = getContentEnd ( code, pos );
            final String word = code.substring ( wordStart, wordEnd );

            if ( word.equals ( trueString ) || word.equals ( falseString ) )
            {
                return new LinkGeneratorResult ()
                {
                    @Override
                    public HyperlinkEvent execute ()
                    {
                        source.replaceRange ( word.equals ( trueString ) ? falseString : trueString, wordStart, wordEnd );
                        return new HyperlinkEvent ( this, HyperlinkEvent.EventType.EXITED, null );
                    }

                    @Override
                    public int getSourceOffset ()
                    {
                        return wordStart;
                    }
                };
            }
            else
            {
                try
                {
                    final Color color = ( Color ) colorConverter.fromString ( word );
                    return color != null ? new LinkGeneratorResult ()
                    {
                        @Override
                        public HyperlinkEvent execute ()
                        {
                            try
                            {
                                final WebPopOver colorChooser = new WebPopOver ( StyleEditor.this );
                                colorChooser.setCloseOnFocusLoss ( true );
                                colorChooser.setStyleId ( "color-pop-over" );

                                final WebColorChooserPanel colorChooserPanel = new WebColorChooserPanel ( false );
                                colorChooserPanel.setColor ( color );
                                colorChooserPanel.addChangeListener ( new ChangeListener ()
                                {
                                    private int length = wordEnd - wordStart;

                                    @Override
                                    public void stateChanged ( final ChangeEvent e )
                                    {
                                        final Color newColor = colorChooserPanel.getColor ();
                                        if ( newColor != null && !newColor.equals ( color ) )
                                        {
                                            final String colorString = colorConverter.toString ( newColor );
                                            source.replaceRange ( colorString, wordStart, wordStart + length );
                                            length = colorString.length ();
                                        }
                                    }
                                } );
                                colorChooser.add ( colorChooserPanel );

                                final Rectangle wb = source.getUI ().modelToView ( source, ( wordStart + wordEnd ) / 2 );
                                colorChooser.show ( source, wb.x, wb.y, wb.width, wb.height, PopOverDirection.down );

                                return new HyperlinkEvent ( this, HyperlinkEvent.EventType.EXITED, null );
                            }
                            catch ( final BadLocationException e )
                            {
                                e.printStackTrace ();
                                return null;
                            }
                        }

                        @Override
                        public int getSourceOffset ()
                        {
                            return wordStart;
                        }
                    } : null;
                }
                catch ( final Throwable e )
                {
                    return null;
                }
            }
        }

        public int getContentStart ( final String text, final int location )
        {
            int wordStart = location;
            while ( wordStart > 0 && !text.substring ( wordStart - 1, wordStart ).equals ( contentStartTag ) )
            {
                wordStart--;
            }
            return wordStart;
        }

        public int getContentEnd ( final String text, final int location )
        {
            int wordEnd = location;
            while ( wordEnd < text.length () - 1 && !text.substring ( wordEnd, wordEnd + 1 ).equals ( contentEndTag ) )
            {
                wordEnd++;
            }
            return wordEnd;
        }
    }

    /**
     * StyleEditor main method used to launch editor.
     *
     * @param args arguments
     */
    public static void main ( final String[] args )
    {
        WebLookAndFeel.install ();

        // Custom StyleEditor skin for WebLaF
        StyleManager.applySkin ( new StyleEditorSkin () );

        // Displaying StyleEditor
        final StyleEditor styleEditor = new StyleEditor ();
        styleEditor.setVisible ( true );
    }
}