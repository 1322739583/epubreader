import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.sun.java.accessibility.util.Translator;
import com.sun.java.swing.plaf.motif.MotifTreeCellRenderer;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubReader;
import sun.reflect.generics.tree.Tree;


import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.DefaultMenuLayout;
import javax.swing.plaf.synth.SynthMenuBarUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.*;
import java.awt.*;
import java.io.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpubBook {

    static JFrame frame;//主窗口
    private JPanel menuPanel;//菜单面板
    // private JPanel contentPanal;//目录面板
    private JPanel readPanel;//主文本面板
    private JSplitPane jSplitPane;//用于分割目录面板和主文本面板
    private Box contentBox = Box.createVerticalBox();//用于存放目录
    private Component box=Box.createVerticalGlue();
    private JScrollPane contentScrollPane;
    private Book book;
    private String bookPath;
    private int contentPanelHeight=2500;
    private int contentPanelWidth=300;
    static private JTree rootTree;
    private Map<String, String> idMap;

    private String htmlData;
    private JTextPane textPane;


    public static void main(String[] args) {
        FlatDarculaLaf.install();
     //   FlatIntelliJLaf.install();
      //  FlatLightLaf.install();
        EpubBook epubBook = new EpubBook();
        frame = new JFrame();
      //  frame.setLayout(new FlowLayout());
        //1.添加菜单
        epubBook.addMenu();
        //2.添加目录和主文本区
        //epubBook.addReadPanel();
        epubBook.getEpub();



        //3.添加主文本区
        frame.pack();
        //frame.setSize(new Dimension(1000,700));
        frame.setVisible(true);
        expandTree(rootTree);
    }

    private static InputStream getResource(String path) {
        return Translator.class.getResourceAsStream(path);
    }

    private static Resource getResource(String path, String href) {
        Resource resource = null;
        try {
            resource = new Resource(getResource(path), href);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resource;
    }

    /**
     * 解析epub文件
     */
    private void getEpub() {

        EpubReader reader = new EpubReader();
        try {
            //1.获取目录内容
            getEpubContent(reader);
            //2.添加目录和文本面板
            addContentAndReadPanel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取epub文件目录
     *
     * @param reader
     * @throws IOException
     */
    private void getEpubContent(EpubReader reader) throws IOException {
        //
        bookPath = "1.epub";
        book = reader.readEpub(new FileInputStream(bookPath));
        TableOfContents tableOfContents = book.getTableOfContents();
        List<TOCReference> tocReferences = tableOfContents.getTocReferences();
        solveTitles(tocReferences, tableOfContents.calculateDepth() );
        solveGuide();

        //System.out.println("3href:"+tableOfContents.getTocReferences().get(3).getCompleteHref());
     //   System.out.println("chapter1 href:"+tableOfContents.getTocReferences().get(3).getChildren().get(0).getCompleteHref());
       // System.out.println("chapter1 res id:"+tableOfContents.getTocReferences().get(3).getChildren().get(0).getResourceId());
       // System.out.println("chapter1 res :"+tableOfContents.getTocReferences().get(3).getChildren().get(0).getResource());
        //System.out.println("chapter1 fragment id:"+tableOfContents.getTocReferences().get(3).getChildren().get(0).getFragmentId());//一个html里面的锚点id
   //     byte[] data = book.getContents().get(6).getData();


//        InputStream in=new ByteArrayInputStream(data);
//        InputStreamReader readerStr=new InputStreamReader(in,"utf-8");
//        StringBuilder htmlContent=new StringBuilder();
//        for (int i = 0; i <data.length ; i++) {
//            int read = readerStr.read();
//            htmlContent.append((char)read);
//        }
        // System.out.println(htmlContent.toString());


    }

    /**
     * 这个只有封面会用到，不是常用的
     */
    private void solveGuide() {
        Guide guide = book.getGuide();
        for (GuideReference reference : guide.getReferences()) {
            System.out.println(reference.getTitle());
            System.out.println(reference.getCompleteHref());
            System.out.println(reference.getFragmentId());
        }
    }

    /**
     * 添加目录和文本面板
     */
    private void addContentAndReadPanel() {
        addReadPanel();
        addContentPanel();


        //
        jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        jSplitPane.setMaximumSize(new Dimension(contentPanelWidth,contentPanelHeight));
         jSplitPane.setMinimumSize(new Dimension(contentPanelWidth,contentPanelHeight));
        jSplitPane.setLeftComponent(contentScrollPane);
        jSplitPane.setRightComponent(readPanel);
       // JPanel tempPanel=new JPanel();
       // tempPanel.add(jSplitPane);
        frame.add(jSplitPane, BorderLayout.CENTER);
    }

    private void addContentPanel() {
       JPanel contentPanel=new JPanel();
          contentPanel.setMaximumSize(new Dimension(contentPanelWidth, contentPanelHeight));
       contentPanel.setMinimumSize(new Dimension(contentPanelWidth, contentPanelHeight));
        contentPanel.add(contentBox);
        contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setMaximumSize(new Dimension(contentPanelWidth, contentPanelHeight));
         contentScrollPane.setMinimumSize(new Dimension(0, contentPanelHeight));

    }

    /**
     * 递归解析目录文本
     *
     * @param tocReferences
     * @param depth
     */
    public void solveTitles(List<TOCReference> tocReferences, int depth ) {
        DefaultMutableTreeNode rootNode=new DefaultMutableTreeNode("" );
        rootTree = new JTree(rootNode );


        initTree(rootTree);



        idMap = new HashMap();

       // for (int i = 0; i < depth; i++) {
            for (TOCReference tocReference : tocReferences) {

                List<TOCReference> children = tocReference.getChildren();
                if (children != null) {
                    DefaultMutableTreeNode levelOneNode=new DefaultMutableTreeNode();
                    levelOneNode.setUserObject(tocReference.getTitle());
                    rootNode.add(levelOneNode);
                    idMap.put(tocReference.getTitle(),tocReference.getResourceId());
//                    JTree tree=new JTree() ;
//                    initTree( tree);
//                    tree.setModel(new DefaultTreeModel(levelOneNode));


                    for (TOCReference subChild : children) {
                        DefaultMutableTreeNode subNode=new DefaultMutableTreeNode();
                        subNode.setUserObject(subChild.getTitle());
                    //    System.out.println(subChild.getTitle());
                        levelOneNode.add(subNode);
                        idMap.put(subChild.getTitle(),subChild.getResourceId());

                        List<TOCReference> subSubChilden = subChild.getChildren();
                        for (TOCReference subSubChild : subSubChilden) {
                            DefaultMutableTreeNode subSubNode=new DefaultMutableTreeNode();
                            subSubNode.setUserObject(subSubChild.getTitle());
                            idMap.put(subSubChild.getTitle(),subSubChild.getResourceId());

                            //    System.out.println(subSubChild.getTitle());
                            subNode.add(subSubNode);
                        }

                        //  contentBox.add(tree);
                    }



                  //  System.out.println(tocReference.getTitle());


                } else {
                    //第一级目录
//                    JTree tree=new JTree() ;
//                    initTree( tree);
                    DefaultMutableTreeNode levelOneNode=new DefaultMutableTreeNode();
                    levelOneNode.setUserObject(tocReference.getTitle());
                    idMap.put(tocReference.getTitle(),tocReference.getResourceId());

                    rootNode.add(levelOneNode);
                    //tree.setModel(new DefaultTreeModel(levelOneNode));

                    //contentBox.add(tree);
                  //  System.out.println(tocReference.getTitle());

                }

              //  TreePath pathForLocation = rootTree.getPathForLocation(0, 0);
               // TreePath pathForRow = rootTree.getPathForRow(-1);
               // rootTree.expandRow(3);
             // expandTree(rootTree );
              //  rootTree.expandPath(pathForRow);
              //  rootTree.expandPath(new TreePath("root"));



                contentBox.add(rootTree);

            }

        System.out.println(idMap);


      //  }


        //  System.out.println("dep=" + depth);
//        if (depth > 0) {
//
//            for (TOCReference tocReference : tocReferences) {
//
//                List<TOCReference> children = tocReference.getChildren();
//                if (children != null) {
//                    levelOneNode.setUserObject(tocReference.getTitle());
//                    JTree tree = new JTree(levelOneNode);
//                    initTree(frame, tree);
//                    System.out.println(tocReference.getTitle());
//                    contentBox.add(tree);
//                    solveTitles(children, depth - 1,levelOneNode);
//                    DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(tocReference.getTitle());
//                  //  levelOneNode.add(subNode);
//
//                } else {
//                    //第一级目录
//                    levelOneNode.setUserObject(tocReference.getTitle());
//                     JTree tree = new JTree(levelOneNode);
//                    initTree(frame, tree);
//                    //contentBox.add(tree);
//                    System.out.println(tocReference.getTitle());
//                }
//            }
//        } else {
//
//        }

    }

    public static void expandTree(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), true);
    }


    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }







    /**
     * 添加文本面板
     */
    private void addReadPanel() {
        readPanel = new JPanel();
        readPanel.setBackground(new Color(207, 226, 40));
        textPane = new JTextPane();
        HTMLDocument text_html;
        HTMLEditorKit htmledit;

        htmledit=new HTMLEditorKit();


        //实例化一个HTMLEditorkit工具包，用来编辑和解析用来显示在jtextpane中的内容。
        text_html=(HTMLDocument) htmledit.createDefaultDocument();

        //使用HTMLEditorKit类的方法来创建一个文档类，HTMLEditorKit创建的类型默认为htmldocument。
        textPane.setEditorKit(htmledit);
        //设置jtextpane组件的编辑器工具包，是其支持html格式。
       // textPane.setContentType("text/html;");

        //设置编辑器要处理的文档内容类型，有text/html,text/rtf.text/plain三种类型。
        textPane.setDocument(text_html);
        //设置编辑器关联的一个文档。

        SimpleAttributeSet attr=new SimpleAttributeSet();

    //实例化一个simpleAttributeSet类。
        StyleConstants.setAlignment(attr, StyleConstants.ALIGN_LEFT);
        //使用StyleConstants工具类来设置attr属性，这里设置居中属性。
        // textpane.setParagraphAttributes(attr,false);
        //设置段落属性，第二个参数为false表示不覆盖以前的属性，如果选择true，会覆盖以前的属性。

        //  StyleConstants.setAlignment(attr, StyleConstants.ALIGN_LEFT);
        //设置属性居左


        // Color color=JColorChooser.showDialog(null,"color title", Color.BLACK);
//使用JColorChooser组件来提供一个颜色选择框。并返回选择的颜色，最后一个参数是缺省颜色。

        //  StyleConstants.setForeground(attr, color);
        //设置颜色属性，参数为color类型。
      //  textPane.setCharacterAttributes(attr, false);

        //            bookPath = "1.epub";
//            book = reader.readEpub(new FileInputStream(bookPath));
//            Book book=new Book();
        // htmledit.insertHTML(text_html, textpane.getCaretPosition(),
        //    "<img src='http://pic1.sc.chinaz.com/Files/pic/icons128/4803/xplorer.png'>", 0, 0, HTML.Tag.IMG);

        //  String  htmlData = getHtmlStringByReferenceId();


        System.out.println(htmlData );
        String xmlTag="<?xml version='1.0' encoding='utf-8'?>";


        textPane.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        if (htmlData!=null) {
            System.out.println("htmldata length"+htmlData.length());
            textPane.setText(htmlData.substring(xmlTag.length()));
        }


        textPane.setEditable(true);
        //设置光标颜色
        textPane.setCaretColor(new Color(232, 239, 69));
        readPanel.setLayout(new BorderLayout(20, 20));
        readPanel.add(textPane);
    }

    private String  getHtmlStringByReferenceId(String refId) throws IOException {
        //一个Content对应一个html文件。
     //   byte[] data = book.getContents().get(6).getData();
        Resources resources = book.getResources();
        byte[] data = resources.getById(refId).getData();
//        System.out.println("...............");
//        for (int i = 0; i < book.getContents().size(); i++) {
//            System.out.println(book.getContents().get(i).getId());
//        }
        InputStream in=new ByteArrayInputStream(data);
        InputStreamReader reader=new InputStreamReader(in,"utf-8");
        StringBuilder htmlData=new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            htmlData.append((char)reader.read());
        }
        return htmlData.toString();
    }


    /**
     * tree结构初始化设置
     *
     * @param tree
     */
    private   void initTree( JTree tree) {
         DefaultTreeCellRenderer render = new DefaultTreeCellRenderer();

        //MotifTreeCellRenderer mrender=new MotifTreeCellRenderer();

        // 设置节点 展开 和 折叠 状态显示的图标
        render.setOpenIcon(null);
        render.setClosedIcon(null);
        render.setLeafIcon(null);
        render.setBackgroundSelectionColor(Color.YELLOW);
        render.setBorderSelectionColor(Color.GREEN);
        render.setBackgroundNonSelectionColor(Color.CYAN);
        render.setTextSelectionColor(Color.PINK);
        render.setForeground(Color.magenta);




        //render.setBackground(Color.ORANGE);//没效果
       // render.setBackgroundSelectionColor(Color.GREEN);//没效果


       //  render.setOpaque(true);//设置这个后文本不会有选择背景色，非常的难看

        tree.setCellRenderer(render);
       // tree.setDropMode(DropMode.USE_SELECTION);//没效果
       // tree.setBackground(Color.magenta);
        tree.setRootVisible(true);


        // 设置树显示根节点句柄
        tree.setShowsRootHandles(false);//会显示第一级目录的图标
        // 设置树节点可编辑
        tree.setEditable(false);

        tree.setPreferredSize(new Dimension(contentPanelWidth, contentPanelHeight));
        tree.setMaximumSize(new Dimension(contentPanelWidth, contentPanelHeight));
        tree.setMinimumSize(new Dimension(contentPanelWidth, contentPanelHeight));
       // tree.setDragEnabled(false);
      //  tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);



        // 设置节点选中监听器
        rootTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                System.out.println("当前被选中的节点: " + e.getPath());
                TreePath path = e.getPath();
                System.out.println(path.getLastPathComponent());
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (idMap!=null){

                    String id = idMap.get((String) node.getUserObject());
                    try {
                       htmlData= getHtmlStringByReferenceId(id);
                        System.out.println("addTreeSelectionListener html length="+htmlData);
                        textPane.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                        String xmlTag="<?xml version='1.0' encoding='utf-8'?>";
                        if (htmlData!=null) {
                            System.out.println("htmldata length"+htmlData.length());
                            textPane.setText(htmlData.substring(xmlTag.length()));
                        }

                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    System.out.println("id is:"+id);
                    //idMap.get()
                }

            }
        });





    }

    /**
     * 添加菜单面板
     */
    private void addMenu() {
        //1.菜单部分
        menuPanel = new JPanel();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件");
        JMenu editMenu = new JMenu("编辑");
        JMenuItem autoItem = new JMenuItem("自动换行");
        JMenuItem copyItem = new JMenuItem("复制");
        JMenuItem pasteItem = new JMenuItem("粘贴");
        JMenu formatMenu = new JMenu("格式");
        JMenuItem commentItem = new JMenuItem("注释");
        JMenuItem uncommentItem = new JMenuItem("取消注释");

        editMenu.add(autoItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        formatMenu.add(commentItem);
        formatMenu.add(uncommentItem);
        editMenu.add(formatMenu);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuPanel.add(menuBar);
        frame.add(menuPanel, BorderLayout.NORTH);
    }
}
