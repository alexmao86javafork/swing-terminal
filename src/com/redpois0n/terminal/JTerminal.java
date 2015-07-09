package com.redpois0n.terminal;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

@SuppressWarnings("serial")
public class JTerminal extends JTextPane {
	
	public static final String RESET = "0";
	public static final String BOLD = "1";
	public static final String DIM = "2";
	public static final String UNDERLINED = "4";
	public static final String INVERTED = "7";
	public static final String HIDDEN = "8";

	public static final Font DEFAULT_FONT;
	public static final Color DEFAULT_FOREGROUND = Color.white;
	public static final Color DEFAULT_BACKGROUND = Color.black;
	public static final char NULL_CHAR = '\u0000';
	
	public static final char ESCAPE = 27;
	public static final String UNIX_CLEAR = ESCAPE + "[H" + ESCAPE + "[J";

	public static final Map<String, Color> COLORS = new HashMap<String, Color>();

	static {		
		DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 14);
		COLORS.put("30", Color.black);
		COLORS.put("31", Color.red.darker());
		COLORS.put("32", Color.green.darker());
		COLORS.put("33", Color.yellow.darker());
		COLORS.put("34", Color.blue);
		COLORS.put("35", Color.magenta.darker());
		COLORS.put("36", Color.cyan.darker());
		COLORS.put("37", Color.white);
	}
	
	public static Color getColor(String s) {
		 Color color = DEFAULT_FOREGROUND;
		 
		 boolean bright = s.contains("1;");
		 s = s.replace("1;", "");
		 
		 if (s.endsWith("m")) {
			 s = s.substring(0, s.length() - 1);
		 }
		 
		 if (COLORS.containsKey(s)) {
			 color = COLORS.get(s);
		 }
		 
		 if (bright) {
			 color = color.brighter();
		 }
	
		 return color;
	}
	
	private List<InputListener> inputListeners = new ArrayList<InputListener>();
	
	private int last;
	private StyledDocument doc;
	
	public JTerminal() {
		this.doc = getStyledDocument();
		setFont(DEFAULT_FONT);
		setForeground(DEFAULT_FOREGROUND);
		setBackground(DEFAULT_BACKGROUND);
		setCaret(new TerminalCaret());
		
		addKeyListener(new KeyEventListener());
		addInputListener(new InputListener() {
			@Override
			public void processCommand(JTerminal terminal, String command) {
				
			}
		});
	}

	/**
	 * Gets main key listener
	 * @return
	 */
	public KeyListener getKeyListener() {
		return super.getKeyListeners()[0];
	}
	
	/**
	 * Called when key pressed, checks if character is valid and checks for combinations such as Ctrl+C
	 * @param e
	 */
	public void keyPressed(KeyEvent e) {
		char c = e.getKeyChar();
		
		/*if (ctrl && (int) e.getKeyChar() == 3) {
			for (InputListener l : inputListeners) {
				l.onTerminate(this);
			}
			return;
		}*/
		
		//append(c);
	}
	
	public synchronized void append(String s) {
        last = doc.getLength();

        boolean fg = true;
		Color foreground = DEFAULT_FOREGROUND;
		Color background = DEFAULT_BACKGROUND;
		boolean bold = false;
		boolean underline = false;
		boolean dim = false;
		
		String s1 = "";
		
        for (int cp = 0; cp < s.toCharArray().length; cp++) {
            char c = s.charAt(cp);
            
            if (c == ESCAPE) {
            	append(s1, foreground, background, bold, underline);
                char next = s.charAt(cp + 1);
                
                if (next == '[') {
                	s1 = "";
                	cp++;
                	while ((c = s.charAt(++cp)) != 'm') {
                		s1 += c;
                	}
                	
                	String[] attributes = s1.split(";");

					for (String at : attributes) {
						if (at.equals(RESET) || s1.length() == 0) {
							foreground = DEFAULT_FOREGROUND;
							background = DEFAULT_BACKGROUND;
							fg = true;
							underline = false;
							dim = false;
							bold = false;
						} else if (at.equals(BOLD)) {
							bold = !bold;
						} else if (at.equals(DIM)) {
							dim = !dim;
						} else if (at.equals(INVERTED)) {
							fg = !fg;
						} else if (at.equals(UNDERLINED)) {
							underline = !underline;
						} else if (s1.length() > 0) {
							if (fg) {
								foreground = getColor(at);
							} else {
								background = getColor(at);
							}
							
							if (dim) {
								foreground = foreground.brighter();
							}
						}
					}
                    
                    s1 = "";
                    continue;
                }
            }
            
            s1 += c;
        }
        
        if (s1.length() > 0) {
        	append(s1, foreground, background, bold, underline);
        }
        
        last = doc.getLength();

        setCursorInEnd();
        
	}
	
	 public void append(String s, Color fg, Color bg, boolean bold, boolean underline) { 
		StyleContext sc = StyleContext.getDefaultStyleContext();

		setCursorInEnd();
		
		setCharacterAttributes(sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, fg), false);
		setCharacterAttributes(sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Background, bg), false);
		setCharacterAttributes(sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, bold), false);
		setCharacterAttributes(sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Underline, underline), false);

		replaceSelection(s);
	}
	
	public void setCursorInEnd() {
		setCaretPosition(doc.getLength());
	}
	
	public class KeyEventListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				enter();
			} else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
				//ctrl = true;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
				//ctrl = false;
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {		
			if ((int) e.getKeyChar() != KeyEvent.VK_BACK_SPACE) {
				JTerminal.this.keyPressed(e);
			}
		}
	}
	
	public void enter() {
		String s = getText().substring(last, doc.getLength());
		last = doc.getLength();
		
		for (InputListener l : inputListeners) {
			l.processCommand(this, s);
		}
		
		setCursorInEnd();
	}
	
	public void addInputListener(InputListener listener) {
		inputListeners.add(listener);
	}
	
	public void removeInputListener(InputListener listener) {
		inputListeners.remove(listener);
	}

}
