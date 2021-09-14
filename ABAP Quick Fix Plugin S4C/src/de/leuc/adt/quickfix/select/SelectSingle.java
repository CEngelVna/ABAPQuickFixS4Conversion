package de.leuc.adt.quickfix.select;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.abapblog.adt.quickfix.assist.syntax.statements.IAssistRegex;
import com.abapblog.adt.quickfix.assist.syntax.statements.StatementAssistRegex;
import com.abapblog.adt.quickfix.assist.syntax.statements.move.MoveExact;

import de.leuc.adt.quickfix.Activator;
import de.leuc.adt.quickfix.preferences.EntryParser;
import de.leuc.adt.quickfix.preferences.OrderPreferencesPage.UserEntries;
import de.leuc.adt.quickfix.preferences.PreferenceConstants;
import de.leuc.adt.quickfix.preferences.UserEntry;

public class SelectSingle extends StatementAssistRegex implements IAssistRegex {

    private static final String ORDER_BY_PRIMARY_KEY = "order by primary key.";
//    private static final String WBHI_ORDER = "order by tkonn, tposn.\n";
//    private static final String WBGT_ORDER = "order by doc_type, vbeln, posnr, posnr_sub, gjahr. \"\" old key fields";
//    private static final String WBHF_ORDER = "order by tkonn_from, tposn_from, tposn_sub_from, tkonn_to, tposn_to, tktyp_to.";
//    private static final String WBIT_ORDER = "order by doc_type, doc_nr, doc_year, item, sub_item.";
//    private static final String WBHD_ORDER = "order by tkonn, tposn, tposn_sub.";
//    private static final String ASSO_ORDER1 = "order by tew_type, assoc_step_from, rdoc_nr, rdoc_year, rdoc_bukrs, rposnr, rposnr_sub,";
//    private static final String ASSO_ORDER2 = "         assoc_step_to, adoc_nr, adoc_year, adoc_bukrs, aposnr, aposnr_sub, rec_base.";
//    private static final String EKBE_ORDER = "order by ebeln ebelp zekkn vgabe gjahr belnr buzei.";
//    private static final String VBFA_ORDER = "order by vbelv, posnv, vbeln, posnn, vbtyp_n.";
//    private static final String EINE_ORDER = "order by  infnr, ekorg, esokz, werks.";
//    private static final String KONV_ORDER = "order by knumv, kposn, stunr, zaehk. ";
//    private static final String MVKE_ORDER = "order by matnr, vkorg, vtweg.";
//    private static final String DRAD_ORDER = "order by dokar, doknr, dokvr, doktl, dokob, objky, obzae. \"#EC CI_FLDEXT_OK";

    private static final String selectPattern =
            // select single * from wbhk into @data(result) where tkonn = ''.
            // 1 2 3 4 5 6
            "(?s)(\\s*)select\\s+(single)\\s+(.*)\\s+from\\s+(.*)\\s+into\\s+(.*)\\s+where\\s+(.*)";

    private static final String replaceByNewSelectPattern1 = "select $3 from $4";
    private static final String replaceByNewSelectPattern2 = "into $5";
    private static final String replaceByNewSelectPattern3 = "up to 1 rows";
    private static final String replaceByNewSelectPattern4 = "where";
    private static final String replaceByNewSelectPattern5 = "  $6";
    private static final String replaceByNewSelectPatternEnd = "endselect";
    private String currentTable;
    /**
     * already contains line break
     */
    private String currentIndent;
    private boolean comments = false;
    private int indent_number = 2;

    public SelectSingle(IQuickAssistInvocationContext context) {
        super(context);

        IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors");
        Boolean bool = preferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS,
                true);
        int tabsno = preferences.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 4);
        System.out.println("preferences are: " + bool + tabsno);
    }

    @Override
    public String getChangedCode() {
        String temp = CodeReader.CurrentStatement.replaceAllPattern("(.*)(\r\n)(.*)", "$1$3");
        temp = temp.trim().replaceAll(" +", " "); // condense spaces
        temp = temp.trim().replaceAll("\\R+", ""); // remove line breaks
        // line breaks are added automatically with the indentation prefix
        String comentedOut = getCommentedOutStatement(CodeReader.CurrentStatement.getStatement());
        return comentedOut.concat(temp.replaceFirst(getMatchPattern(), getReplacePattern()) );
    }

    private String getCommentedOutStatement(String in) {
        if (Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.COMMENT_OUT)) {
            return in.replaceAll("(\\R)", "$1*");
        }
        return "";
    }

    @Override
    public String getAssistShortText() {
        return "Replace select single with select up to one rows";
    }

    @Override
    public String getAssistLongText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Image getAssistIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canAssist() {
        if (CodeReader.CurrentStatement.matchPattern(getMatchPattern()) && !(new MoveExact(context).canAssist())) {
            // table name to decide on order by clause
            currentTable = CodeReader.CurrentStatement.replacePattern(getMatchPattern(), "$4").replaceAll("\\R", "").replaceAll("\s", "");
            // get current indentation for select, prefix with linebreak
            String temp = CodeReader.CurrentStatement.replacePattern(getMatchPattern(), "$1");
            currentIndent = "\r\n".concat(temp.replaceAll("\\s*\\R", ""));// remove spaces

            IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            comments = store.getBoolean(PreferenceConstants.ADD_COMMENTS);
            indent_number = store.getInt(PreferenceConstants.INDENT);
            System.out.println("local preferences are: " + comments + " " + indent_number);

            return true;
        }
        return false;
    }

    @Override
    public int getStartOfReplace() {
        return CodeReader.CurrentStatement.getBeginOfStatement();
    }

    @Override
    public int getReplaceLength() {
        return CodeReader.CurrentStatement.getStatementLength();
    }

    @Override
    public String getMatchPattern() {
        return selectPattern;

    }

    @Override
    public String getReplacePattern() {
        String indent = getPrefix();
        StringBuffer temp = new StringBuffer();
        temp.append(currentIndent.concat(getCommentPrefix()));
        temp.append(currentIndent.concat(replaceByNewSelectPattern1));// select
        temp.append(currentIndent.concat(indent).concat(replaceByNewSelectPattern2)); // into
        temp.append(currentIndent.concat(indent).concat(replaceByNewSelectPattern3)); // up to 1 rows
        temp.append(currentIndent.concat(indent).concat(replaceByNewSelectPattern4)); // where
        temp.append(currentIndent.concat(indent).concat(replaceByNewSelectPattern5)); // statement

        // order by depends on table
        // several tables feature uuids as keys - use old key fields to order lines
        // order by primary key otherwise
        String orderBy = EntryParser.getOrderBy(currentTable);
        
        if (orderBy != null) {
            temp.append(currentIndent.concat(indent).concat("order by " + orderBy + "."));
//        } else if (currentTable.toLowerCase().contains("wbgt")) {
//            temp.append(currentIndent.concat(indent).concat(WBGT_ORDER));
//        } else if (currentTable.toLowerCase().contains("wbhf")) {
//            temp.append(currentIndent.concat(indent).concat(WBHF_ORDER));
//        } else if (currentTable.toLowerCase().contains("ekbe")) {
//            temp.append(currentIndent.concat(indent).concat(EKBE_ORDER));
//        } else if (currentTable.toLowerCase().contains("vbfa")) {
//            temp.append(currentIndent.concat(indent).concat(VBFA_ORDER));
//        } else if (currentTable.toLowerCase().contains("konv")) {
//            temp.append(currentIndent.concat(indent).concat(KONV_ORDER));
//        } else if (currentTable.toLowerCase().contains("drad")) {
//            temp.append(currentIndent.concat(indent).concat(DRAD_ORDER));
//        } else if (currentTable.toLowerCase().contains("mvke")) {
//            temp.append(currentIndent.concat(indent).concat(MVKE_ORDER));
//        } else if (currentTable.toLowerCase().contains("wbhd")) {
//            temp.append(currentIndent.concat(indent).concat(WBHD_ORDER));
//        } else if (currentTable.toLowerCase().contains("wbit")) {
//            temp.append(currentIndent.concat(indent).concat(WBIT_ORDER));
//        } else if (currentTable.toLowerCase().contains("eine")) {
//            temp.append(currentIndent.concat(indent).concat(EINE_ORDER));
//        } else if (currentTable.toLowerCase().contains("wbassoc")) {
//            temp.append(currentIndent.concat(indent).concat(ASSO_ORDER1 + currentIndent + ASSO_ORDER2));
////		} else if(currentTable.toLowerCase().contains("")){
////			temp.append( currentIndent.concat("  ") );
        } else {
            temp.append(currentIndent.concat(indent).concat(ORDER_BY_PRIMARY_KEY));
        }
        temp.append(currentIndent.concat(replaceByNewSelectPatternEnd));
        return temp.toString();

    }

    private String getCommentPrefix() {
        if (comments) {
            String comment = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.COMMENT_TEXT);
            return comment.replace("${DATE}", java.time.LocalDateTime.now().toString());
        }
        return "";
    }

    private String getPrefix() {
        return String.format("%" + indent_number + "s", "");
    }

}
