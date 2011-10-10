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
/**
 * Serializes a JSON string to a UTF-8 encoded byte stream.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public abstract class AbstractJSONSerializer {
  
  protected abstract void   write(int b);
  protected abstract Object error(String text);
  
  // ------------------------------------------------------------------------------------------------------------------
  
  protected void serialize(Object o) {
         if (o instanceof String  )  handleString     ((String)o   );
    else if (o instanceof Number  )  handleASCIIString(o.toString());
    else if (o instanceof Boolean )  handleASCIIString(o.toString());
    else if (o == null            )  handleASCIIString("null"      );
    else if (o instanceof JSONMap )  handleMap        ((JSONMap )o );
    else if (o instanceof JSONList)  handleList       ((JSONList)o );
    else if (o instanceof byte[]  )  handleBytes      ((byte[])o   );
    else  error("could not serialize " + o.getClass().getName() + " object");
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  protected void handleMap(JSONMap map) {
    write('{');
    boolean first = true;
    for (String key:map.keySet()) {
      if (first)  first=false;  else  write(',');
      handleString(key);  write(':');  serialize(map.get(key));
    }
    write('}');
  }
  
  protected void handleList(JSONList list) {
    write('[');
    boolean first = true;
    for (Object element:list) {
      if (first)  first=false;  else  write(',');
      serialize(element);
    }
    write(']');
  }
  
  private void handleBytes(byte[] bb) {
    write('$');
    for (byte b:bb) {
      write(toHex((b>>4)&0xF));
      write(toHex((b   )&0xF));
    }
    write('$');
  }
  
  private char toHex(int i) {
    return "0123456789ABCDEF".charAt(i);
  }
  
  private void handleASCIIString(String s) {
    for (int i=0; i<s.length(); i++)  write(s.charAt(i));
  }
  
  private void handleString(String s) { 
    write('\"');
    for (int i=0; i<s.length(); i++)  handleStringChar(s.charAt(i));
    write('\"');
  }
  
  private void handleStringChar(int c) {
         if (c == '"' ) { write('\\'); write('\"'); }
    else if (c == '\\') { write('\\'); write('\\'); }
    else if (c >= 32  ) {        handleUTF8Char(c); }
    else if (c == '\n') { write('\\'); write('n' ); }
    else if (c == '\t') { write('\\'); write('t' ); }
    else if (c == '\b') { write('\\'); write('b' ); }
    else if (c == '\f') { write('\\'); write('f' ); }
    else if (c == '\r') { write('\\'); write('r' ); }
    else                {              write(c   ); }
  }
  
  private void handleUTF8Char(int c) {
         if (c<0x80   ){                                                                                   write(      c      );}
    else if (c<0x800  ){                                                        write(0xC0|((c>>6)&0x1F)); write(0x80|(c&0x3F));}
    else if (c<0x10000){                            write(0xE0|((c>>12)&0x0F)); write(0x80|((c>>6)&0x3F)); write(0x80|(c&0x3F));}
    else               {write(0xF0|((c>>18)&0x07)); write(0x80|((c>>12)&0x3F)); write(0x80|((c>>6)&0x3F)); write(0x80|(c&0x3F));}
  }
}