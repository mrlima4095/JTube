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
package ui.screens;

import App;
import Settings;
import ui.items.VideoItem;

public class MainScreen extends NavigationScreen {

	public MainScreen() {
		super("", null);
	}
	
	protected void show() {
		super.show();
		if(wasHidden) {
			wasHidden = false;
			if(Settings.videoPreviews) {
				// resume loading previews
				App.inst.stopAsyncTasks();
				boolean b = false;
				for(int i = 0; i < items.size(); i++) {
					Object o = items.elementAt(i);
					if(o instanceof VideoItem) {
						App.inst.addAsyncTask(((VideoItem)o).getVideo());
						b = true;
					}
				}
				if(b) App.inst.startAsyncTasks();
			}
		}
	}
	
	protected void hide() {
		super.hide();
		if(Settings.rmsPreviews) {
			for(int i = 0; i < items.size(); i++) {
				Object o = items.elementAt(i);
				if(o instanceof VideoItem) {
					((VideoItem)o).onHide();
				}
			}
		}
	}
/*
	public void commandAction(Command c, Displayable d) {
		if(c == backCmd) {
			ui.display(null);
			return;
		}
		if(c == optsCmd) {
			ui.showOptions();
			return;
		}
		super.commandAction(c, d);
	}
*/
}
