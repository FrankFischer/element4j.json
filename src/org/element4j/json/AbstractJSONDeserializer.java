package org.element4j.json;
/* 
 * Copyright (c) 2011-2011 Frank Fischer.
 * 
 * This file is part of element4j (see <http://www.element4j.org>).
 * 
 * element4j is free software: you can redistribute it and/or modify it under the terms 
 * of the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * element4j is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with element4j.
 * If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
/**
 * Deserializes a JSON string from a UTF-8 encoded byte stream.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public abstract class AbstractJSONDeserializer {
  
  private   static       ListEnd       listEnd                = new ListEnd();    private static class ListEnd { }
  private   static       MapEnd        mapEnd                 = new MapEnd();     private static class MapEnd  { }
  private   static       Colon         colon                  = new Colon();      private static class Colon   { }
  private   static       Comma         comma                  = new Comma();      private static class Comma   { }
  
  private   static final int           EXPECT_CommaOrMapEnd   =  5;    // %000101
  private   static final int           EXPECT_CommaOrListEnd  =  6;    // %000110
  private   static final int           EXPECT_Colon           =  8;    // %001000
  private   static final int           EXPECT_Key             = 16;    // %010000
  private   static final int           EXPECT_KeyOrMapEnd     = 17;    // %010001
  private   static final int           EXPECT_Object          = 32;    // %100000
  private   static final int           EXPECT_ObjectOrListEnd = 34;    // %100010
  
  private   static final int           MASK_NONE              =  0;
  private   static final int           MASK_mapEnd            =  1;
  private   static final int           MASK_listEnd           =  2;
  private   static final int           MASK_comma             =  4;
  private   static final int           MASK_colon             =  8;
  private   static final int           MASK_mapKey            = 16;
  private   static final int           MASK_OBJECT            = 32;
  private   static final int           MASK_STRING            = MASK_mapKey + MASK_OBJECT;
  
  private   static final int           BUF_LEN                = 1024;
  private                byte[]        buffer;                         // buffers the incoming bytes
  private                int           len;                            // the number of bytes written to the buffer
  private                int           pnt;                            // pointer to the next read position in the buffer
  private                int           c;                              // the actual character
  private                boolean       alreadyRead;                    // the actual character has been read already
  
  private   static       StringBuilder sbuffer;
  private   static       StringBuilder unicode;
  protected static       Action[]      action;
  private   static       int[]         numtab;
  private                int[]         hextab;
  
  private   static       LinkedList<Object> collectionStack;
  
  // ------------------------------------------------------------------------------------------------------------------
  
  protected abstract Object error(String text) throws JSONException;
  protected abstract int    getBytes(byte[] b) throws JSONException;
  
  // ------------------------------------------------------------------------------------------------------------------
  
  private int read() throws JSONException {
    if (alreadyRead) { alreadyRead = false;  return c; }
    
    if (pnt >= len) {
      len = getBytes(buffer);
      if (len <= 0)  return c = -1;
      pnt = 0;
    }
    return  c = buffer[pnt++] & 0xFF;
  }
  
  protected void unread() { alreadyRead = true; }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  private String whatWasExpected(int expect) {
    switch (expect) {
      case EXPECT_CommaOrMapEnd  : return "',' or '}'";
      case EXPECT_CommaOrListEnd : return "',' or ']'";
      case EXPECT_Colon          : return "':'";
      case EXPECT_Key            : return "a map key (string)";
      case EXPECT_KeyOrMapEnd    : return "a map key (string) or '}'";
      case EXPECT_Object         : return "some object";
      case EXPECT_ObjectOrListEnd: return "some object or ']'";
      default                    : return "i don't know what was expected (this should not happen)";
    }
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  private void   listStart()                        { collectionStack.push(new JSONList()); }
  private void   mapStart()                         { collectionStack.push(new JSONMap ()); }
  private void   addToList(Object value)            { ((JSONList)collectionStack.getFirst()).add(value);      }
  private void   addToMap(String key, Object value) { ((JSONMap )collectionStack.getFirst()).put(key, value); }
  private Object listDone()                         { return collectionStack.pop(); }
  private Object mapDone()                          { return collectionStack.pop(); }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  public AbstractJSONDeserializer() {
    action = new Action[128];
    Action errorAction = new aError();
    Action numAction   = new aNumber();
    for (int i=0; i<128; i++               )  action[i] = errorAction;
    for (char c:"0123456789-".toCharArray())  action[c] = numAction;
    action['t'] = new aTrue();
    action['f'] = new aFalse();
    action['n'] = new aNull();
    action['N'] = new aNaN();
    action['['] = new aList();
    action['{'] = new aMap();
    action['$'] = new aBytes();
    action['"'] = new aString();
    action[','] = new aComma();
    action[':'] = new aColon();
    action[']'] = new aEndList();
    action['}'] = new aEndMap();
    
    numtab = new int[128];
    for (int i=0; i<128; i++               )  numtab[i] = 0;
    for (char c:        ".eE".toCharArray())  numtab[c] = 2;
    for (char c:"0123456789-".toCharArray())  numtab[c] = 1;
    
    sbuffer         = new StringBuilder(1024);
    unicode         = new StringBuilder(4);
    collectionStack = new LinkedList<Object>();
    
    hextab = new int[256];
    for (int i=0; i<256; i++)  hextab[i] = -1;
    for (int i=0; i< 10; i++)  hextab["0123456789".charAt(i)] = i;
    for (int i=0; i<  6; i++)  hextab["ABCDEF"    .charAt(i)] = i+10;
    for (int i=0; i<  6; i++)  hextab["abcdef"    .charAt(i)] = i+10;
    
    buffer  = new byte[BUF_LEN];
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  // ------------------------------------------------------------------------------------------------------------------
  // ------------------------------------------------------------------------------------------------------------------
  
  protected Object deserialize() throws JSONException {
    alreadyRead = false;
    len         = BUF_LEN;
    pnt         = len;
    collectionStack.clear();
    return getObject(EXPECT_Object);
  }
  
  private Object getObject(int expect) throws JSONException {
    skipWhitespaceAndComments();
    if (c >= 128)  error("unexpected character '" + c + "', expected was: " + whatWasExpected(expect));
    Action a = action[c];
    if ((a.mask & expect) == 0)  error("unexpected character '" + (char)c + "', expected was: " + whatWasExpected(expect));
    return a.read();
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  protected abstract class Action {
    private  int    mask;
    public          Action(int mask) { this.mask = mask; }
    abstract Object read() throws JSONException;
  }
  
  protected class aError   extends Action{aError  (){super(MASK_NONE   );} @Override Object read() throws JSONException { return error("error"); }}
  protected class aTrue    extends Action{aTrue   (){super(MASK_OBJECT );} @Override Object read() throws JSONException { skip("true" ); return true;       }}
  protected class aFalse   extends Action{aFalse  (){super(MASK_OBJECT );} @Override Object read() throws JSONException { skip("false"); return false;      }}
  protected class aNull    extends Action{aNull   (){super(MASK_OBJECT );} @Override Object read() throws JSONException { skip("null" ); return null;       }}
  protected class aNaN     extends Action{aNaN    (){super(MASK_OBJECT );} @Override Object read() throws JSONException { skip("NaN"  ); return Double.NaN; }}
  protected class aEndMap  extends Action{ aEndMap(){super(MASK_mapEnd );} @Override Object read() throws JSONException { return mapEnd;                    }}
  protected class aEndList extends Action{aEndList(){super(MASK_listEnd);} @Override Object read() throws JSONException { return listEnd;                   }}
  protected class aColon   extends Action{aColon  (){super(MASK_colon  );} @Override Object read() throws JSONException { return colon;                     }}
  protected class aComma   extends Action{aComma  (){super(MASK_comma  );} @Override Object read() throws JSONException { return comma;                     }}
  protected class aString  extends Action{aString (){super(MASK_STRING );} @Override Object read() throws JSONException { return readString();              }}
  protected class aNumber  extends Action{aNumber (){super(MASK_OBJECT );} @Override Object read() throws JSONException { return readNumber();              }}
  protected class aBytes   extends Action{aBytes  (){super(MASK_OBJECT );} @Override Object read() throws JSONException { return readBytes();               }}
  protected class aMap     extends Action{aMap    (){super(MASK_OBJECT );} @Override Object read() throws JSONException { return readMap();                 }}
  protected class aList    extends Action{aList   (){super(MASK_OBJECT );} @Override Object read() throws JSONException { return readList();                }}
  
  // ------------------------------------------------------------------------------------------------------------------
  /** set c to the first non-whitespace-char (or 0 if end of string was reached). 
   * @throws JSONException */
  private void skipWhitespaceAndComments() throws JSONException {
    while (read() >= 0) {
      if ((c != ' ' ) && (c != '\n') && (c != '\t') && (c != '#'))  return;
      if (c == '#' )  skipLine();
    }
    c = 0;
  }
  
  private void skipLine() throws JSONException {
    while (read() >= 0) {       // skip till end of line (or end of json string)
      if (c == '\n')  break;
    }
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  private Object readMap() throws JSONException {
    Object value;
    mapStart();
    value = getObject(EXPECT_KeyOrMapEnd);
    if (value instanceof MapEnd) return mapDone();
    getObject(EXPECT_Colon);
    addToMap((String)value, getObject(EXPECT_Object));
    do {
      value = getObject(EXPECT_CommaOrMapEnd);
      if (value instanceof MapEnd) return mapDone();
      value = getObject(EXPECT_Key);
      getObject(EXPECT_Colon);
      addToMap((String)value, getObject(EXPECT_Object));
    } while (true);
  }
  
  private Object readList() throws JSONException {
    Object value;
    listStart();
    value = getObject(EXPECT_ObjectOrListEnd);
    if (value instanceof ListEnd) return listDone();
    addToList(value);
    do {
      value = getObject(EXPECT_CommaOrListEnd);
      if (value instanceof ListEnd) return listDone();
      value = getObject(EXPECT_Object);
      addToList(value);
    } while (true);
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  private Object readBytes() throws JSONException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    while (read() >= 0) {
      if (c == '$')  return bos.toByteArray();
      if (c < 0)  error("expected hexadecimal character");
      int b = hextab[c] << 4;
      
      if (read() < 0)  error("premature end of JSON string");
      if (c < 0)  error("expected hexadecimal character");
      bos.write(b | hextab[c]);
    }
    return error("premature end of JSON string");
  }
  
  private Object readNumber() throws JSONException {
    int           num;
    boolean       isDouble = false;
    StringBuilder sb       = new StringBuilder();
    unread();
    while (read() >= 0) {
      if (c >= 128)  error("did not expected a non ASCII character in a number");
      num = numtab[c];
      if (num == 2)  isDouble = true;
      if (num == 0)  { unread(); break; }
      sb.append((char)c);
    }
    try {
      if (isDouble)  return Double.parseDouble(sb.toString());
               else  return Long.parseLong(sb.toString());
    } catch (NumberFormatException e) {
      return error(e.toString());
    }
  }
  
  private Object readString() throws JSONException {
    sbuffer.setLength(0);
    while (read() >= 0) {
      if (c == '"' ) {
        return sbuffer.toString();
      } else if (c == '\\') {
        if (read() < 0)  error("premature end of JSON string - expected escape sequence");
        manageEscapedChar();
      } else {
        sbuffer.append(unicodify(c & 0xFF));
      }
    }
    return error("end of json before end of string");
  }
  
  private char unicodify(int b) throws JSONException {
         if (b < 0x80) {                                                    return (char)b;                      }
    else if (b < 0xE0) {                             read(); int b2=c&0x3F; return (char)(((b&0x1F)<< 6)|b2   ); }
    else if (b < 0xF0) { read(); int b2=(c&0x3f)<<6; read(); int b3=c&0x3F; return (char)(((b&0x0F)<<12)|b2|b3); }
    else               { read(); read(); read(); return '\uFFFD'; }  // not implemented --> replacement character
  }
  
  private void manageEscapedChar() throws JSONException {
    switch (c) {
      case '\\':  sbuffer.append('\\');  break;
      case '\'':  sbuffer.append('\'');  break;
      case '\"':  sbuffer.append('"' );  break;
      case 'r' :  sbuffer.append('\r');  break;
      case 'f' :  sbuffer.append('\f');  break;
      case 't' :  sbuffer.append('\t');  break;
      case 'n' :  sbuffer.append('\n');  break;
      case 'b' :  sbuffer.append('\b');  break;
      case 'u' :  manageUnicodeEscapedSequence();  break;
      default  :  sbuffer.append(c);     break;
    }
  }
  
  private void manageUnicodeEscapedSequence() throws JSONException {
    unicode.setLength(0);
    for (int i=0; i<4; i++) {
      if (read() < 0)  error("premature end of JSON string");
      unicode.append((char)c);
    }
    try {
      sbuffer.append((char)Integer.parseInt(unicode.toString(), 16));
    } catch (NumberFormatException nfe) { error("unable to parse unicode value '" + unicode.toString() + "'"); }
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  /** Skip a String from the input (but check if it is there). */
  private void skip(String s) throws JSONException {
    int pnt2 = 1;
    while (read() >= 0) {
      if (c != s.charAt(pnt2++))  error("expected '" + s + "'");
      if (pnt2 >= s.length())  return;
    }
    error("unexpected end, expexted '" + s + "'");
  }
}