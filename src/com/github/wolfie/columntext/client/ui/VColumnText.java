package com.github.wolfie.columntext.client.ui;

import java.util.Stack;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;

public class VColumnText extends Composite implements Paintable {
  
  /** Set the CSS class name to allow styling. */
  public static final String CLASSNAME = "v-columntext";
  
  public static final String COLUMNS_INT = "cols";
  public static final String TEXT_STRING = "text";
  public static final String IS_TEXT_BOOL = "istext";
  
  /** The client side widget identifier */
  protected String paintableId;
  
  /** Reference to the server connection object. */
  ApplicationConnection client;
  
  private final CellPanel panel = new HorizontalPanel();
  
  private String text = "";
  
  private int columns;
  
  private boolean isText;
  
  /**
   * The constructor should first call super() to initialize the component and
   * then handle any initialization relevant to Vaadin.
   */
  public VColumnText() {
    initWidget(panel);
    
    // This method call of the Paintable interface sets the component
    // style name in DOM tree
    setStyleName(CLASSNAME);
  }
  
  /**
   * Called whenever an update is received from the server
   */
  public void updateFromUIDL(final UIDL uidl, final ApplicationConnection client) {
    if (client.updateComponent(this, uidl, true)) {
      return;
    }
    this.client = client;
    paintableId = uidl.getId();
    
    boolean needsRecalculation = false;
    if (uidl.hasAttribute(COLUMNS_INT)) {
      columns = uidl.getIntAttribute(COLUMNS_INT);
      needsRecalculation = true;
    }
    
    if (uidl.hasAttribute(TEXT_STRING)) {
      text = uidl.getStringAttribute(TEXT_STRING);
      needsRecalculation = true;
    }
    
    if (uidl.hasAttribute(IS_TEXT_BOOL)) {
      isText = uidl.getBooleanAttribute(IS_TEXT_BOOL);
      needsRecalculation = true;
    }
    
    if (needsRecalculation) {
      recalculateColumns();
    }
    
  }
  
  private void recalculateColumns() {
    // TODO: optimize by checking where the string has been changed, and only
    // change the necessary columns.
    
    // TODO: optimize by binary search.
    
    // TODO: optimize by letting each column act as a guess for the next
    // column's word count, and adjust from there.
    
    panel.clear();
    final String columnWidth = (100 / columns) + "%";
    
    if (isText) {
      writeText(columnWidth);
    } else {
      writeHTML(columnWidth);
    }
  }
  
  private void writeHTML(final String columnWidth) {
    // the columns must be defined first, so that they occupy the required
    // horizontal space as they would.
    for (int i = 0; i < columns; i++) {
      final HTML html = new HTML();
      panel.add(html);
      panel.setCellWidth(html, columnWidth);
    }
    
    final Stack<String[]> tagStack = new Stack<String[]>();
    final String[] tokens = SplitUtil.splitHTML(text);
    final int widgetHeight = getOffsetHeight();
    int column = 0;
    HTML html = (HTML) panel.getWidget(column);
    String currentHTML = "";
    
    for (final String token : tokens) {
      populateTag(tagStack, token);
      final String endTag = getEndTag(tagStack);
      
      html.setHTML(currentHTML + " " + token + endTag);
      if (html.getOffsetHeight() > widgetHeight) {
        html.setHTML(currentHTML + endTag);
        
        column++;
        if (column >= columns) {
          break;
        }
        
        html = (HTML) panel.getWidget(column);
        currentHTML = getStartTag(tagStack) + token;
      }

      else {
        currentHTML += " " + token;
      }
    }
  }
  
  private static String getEndTag(final Stack<String[]> tagStack) {
    if (!tagStack.isEmpty()) {
      return "</" + tagStack.peek()[1] + ">";
    } else {
      return "";
    }
  }
  
  private static String getStartTag(final Stack<String[]> tagStack) {
    if (!tagStack.isEmpty()) {
      /*
       * whenever we need the start tag, it's because we started a new column,
       * and thus don't need the end tag anymore. FIXME: this will break if a
       * tag spans several columns.
       */
      return tagStack.peek()[0];
    } else {
      return "";
    }
  }
  
  private void populateTag(final Stack<String[]> tagStack, final String token) {
    if (token.startsWith("<")) {
      
      /*
       * for a token like "<a href='foo'>", populate the stack with {
       * "<a href='foo'>", "a" }. This is to later being able to provide a
       * closing tag for the end of a column, and to start again with the same
       * tag on the next column.
       */

      // Sigh. GWT doesn't have decent regexps
      for (int i = 1; i < token.length(); i++) {
        final char c = token.charAt(i);
        if (SplitUtil.isWhitespace(c) || c == '>') {
          tagStack.push(new String[] { token, token.substring(1, i) });
        }
      }
    }
  }
  
  private void writeText(final String columnWidth) {
    // the columns must be defined first, so that they occupy the required
    // horizontal space as they would.
    for (int i = 0; i < columns; i++) {
      final Label label = new Label();
      panel.add(label);
      panel.setCellWidth(label, columnWidth);
    }
    
    final String[] tokens = text.split("\\s");
    final int widgetHeight = getOffsetHeight();
    int column = 0;
    Label label = (Label) panel.getWidget(column);
    for (final String token : tokens) {
      final String currentText = label.getText();
      
      label.setText(currentText + " " + token);
      if (label.getOffsetHeight() > widgetHeight) {
        label.setText(currentText);
        
        column++;
        if (column >= columns) {
          break;
        }
        
        label = (Label) panel.getWidget(column);
        label.setText(token);
      }
    }
  }
}
