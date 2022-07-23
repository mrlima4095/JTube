/*
Copyright (c) 2022 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package ui.nokia_extensions;

import javax.microedition.lcdui.Font;

import cc.nnproject.utils.PlatformUtils;

public class DirectFontUtil {
	
	private static boolean supported;
	
	public static void init() {
		if(PlatformUtils.isSymbian3Based() || PlatformUtils.isAshaFullTouch() || PlatformUtils.isKemulator || PlatformUtils.isJ2ML()) {
			try {
				DirectUtilsInvoker.init();
				supported = true;
			} catch (Exception e) {
			} catch (Error e) {
			}
		}
	}
	
	public static Font getFont(int face, int style, int height, int size) {
		if(supported) {
			try {
				Font f = DirectUtilsInvoker.getFont(face, style, height);
				if(f != null) return f;
			} catch (Throwable e) {
			}
		}
		return Font.getFont(face, style, size);
	}

	public static boolean isSupported() {
		return supported;
	}
	
}

