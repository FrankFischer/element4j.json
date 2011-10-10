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
 * Error message during JSON deserialization.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public class JSONException extends Exception {
  
  private static final long serialVersionUID = 1L;
  
  private String[] json;
  
  /**
   * @return  the first part of the JSON string including the error position: getJSON()[0]<br />
   *          the JSON string after the error position: getJSON()[1]
   */
  public String[] getJSON() { return json; }
  
  /**
   * @param message  description of the error
   * @param json     the first part of the JSON string including the error position: json[0]<br />
   *                 the JSON string after the error position: getJSON()[1]
   */
  public JSONException(String message, String[] json) {
    super(message, null);
    this.json = json;
  }
}