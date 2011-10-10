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
import java.util.LinkedHashMap;
/**
 * A deserialized JSON object.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public class JSONMap extends LinkedHashMap<String,Object> {
  private static final long serialVersionUID = 1L;
  
  public JSONMap(String ... s) { super(); 
    for (int i=0; i<s.length; i+=2)  put(s[i], s[i+1]);
  }
  
  // -----------------------------------------------------------------------------------------------------------------------------
  
  public int getInt(String key) {
    Object o = get(key);
    if (o instanceof Number)  return ((Number)o).intValue();
    return 0;                                                        // TODO: is this correct?
  }
  
  public String getString(String key) {
    Object o = get(key);
    if (o == null)  return null;
    return (String)o;
  }
}