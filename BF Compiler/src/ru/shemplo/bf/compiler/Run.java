package ru.shemplo.bf.compiler;

import static org.objectweb.asm.Opcodes.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class Run {

	private final static String NAME_CL = "Brainfuck";
	
	private final static String NAME_SYS = name (System.class),
								NAME_PRS = name (PrintStream.class),
								NAME_SC  = name (Scanner.class),
								NAME_S   = name (String.class),
								NAME_INS = name (InputStream.class),
								NAME_STA = name (String [].class);
	
	private static ClassWriter cw;
	private static int LINE = 0;
	
	public static void main (String... args) {
		if (args == null || args.length == 0) {
			String message = "Nothing to compile";
			throw new IllegalStateException (message);
		}
		
		File srcFile = new File (args [0]);
		String code = args [0];
		if (srcFile.exists () && srcFile.isFile ()) {
			try (
				InputStream is = new FileInputStream (srcFile);
				Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader (r);
			) {
				StringBuilder sb = new StringBuilder ();
				while ((code = br.readLine ()) != null) {
					sb.append (code);
				}
				
				code = sb.toString ();
			} catch (IOException ioe) {
				System.out.println ("Error occured: " + ioe.getMessage ());
			}
		}
		
		Compiler c = new Compiler (code);
		int [] bounds = c.bounds ();
		
		cw = new ClassWriter (0);
		int CLASS_ACCESS = ACC_PUBLIC + ACC_SUPER;
		cw.visit (V1_8, CLASS_ACCESS, NAME_CL, 
					null, "java/lang/Object", null);
		cw.visitSource (NAME_CL + ".java", null);
		
		declareVaribles ();
		int size = bounds [1] - bounds [0] + 1;
		declareStaticConstructor (size, -bounds [0]);
		
		MethodVisitor mv = null;
		int MAIN_ACCESS = ACC_PUBLIC + ACC_STATIC + ACC_VARARGS;
		mv = cw.visitMethod (MAIN_ACCESS, "main", "(" + NAME_STA + ")V", 
								null, null);
		mv.visitCode ();
		
		c.compile (mv);
		
		mv.visitInsn (RETURN);
		mv.visitMaxs (10, 1);
		mv.visitEnd ();
		
		cw.visitEnd ();
		
		File outFile = new File (NAME_CL + ".class");
		try (
			OutputStream os = new FileOutputStream (outFile);
		) {
			byte [] body = cw.toByteArray ();
			os.write (body, 0, body.length);
		} catch (IOException ioe) {
			ioe.printStackTrace ();
		}
	}
	
	private static String name (Class <?> token) {
		if (token == null) { return "null"; }
		return token.getName ().replace ('.', '/');
	}
	
	private static void declareVaribles () {
		int FIELD_ACCESS = ACC_PRIVATE + ACC_STATIC;
		FieldVisitor fv = null;
		
		/* Scanner for reading input */
		fv = cw.visitField (FIELD_ACCESS + ACC_FINAL, "S", 
							"L" + NAME_SC + ";", null, null);
		fv.visitEnd ();
		
		/* Turing machine tape for simulating */
		fv = cw.visitField (FIELD_ACCESS, "tape", "[I", null, null);
		fv.visitEnd ();
		
		/* Carriage position now */
		fv = cw.visitField (FIELD_ACCESS, "car", "I", null, null);
		fv.visitEnd ();
	}
	
	private static void 
		declareStaticConstructor (int tapeLength, int offset) {
		MethodVisitor mv = null;
		
		mv = cw.visitMethod (ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode ();
		
		/* Initialization of `S` */
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitTypeInsn (NEW, NAME_SC);
		mv.visitInsn (DUP);
		mv.visitFieldInsn (GETSTATIC, NAME_SYS, "in", 
							"L" + NAME_INS + ";");
		mv.visitLdcInsn ("UTF-8");
		String cSign = "(L" + NAME_INS + ";L" + NAME_S + ";)V";
		mv.visitMethodInsn (INVOKESPECIAL, NAME_SC, "<init>", cSign, false);
		mv.visitFieldInsn (PUTSTATIC, NAME_CL, "S", "L" + NAME_SC + ";");
		
		/* Initialization of `tape` */
		Label l1 = new Label ();
		mv.visitLabel (l1);
		mv.visitLineNumber (LINE++, l1);
		tapeLength = Math.max (0, tapeLength);
		mv.visitIntInsn (BIPUSH, tapeLength);
		mv.visitIntInsn (NEWARRAY, T_INT);
		mv.visitFieldInsn (PUTSTATIC, NAME_CL, "tape", "[I");
		
		/* Initialization of `car` */
		Label l2 = new Label ();
		mv.visitLabel (l2);
		mv.visitLineNumber (LINE++, l2);
		offset = Math.max (0, offset);
		mv.visitIntInsn (BIPUSH, offset);
		mv.visitFieldInsn (PUTSTATIC, NAME_CL, "car", "I");
		
		mv.visitInsn (RETURN);
		mv.visitMaxs (5, 0);
		mv.visitEnd ();
	}
	
	public static void actionMoveCarriage (MethodVisitor mv, int offset) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitIntInsn (BIPUSH, offset);
		mv.visitInsn (IADD);
		mv.visitFieldInsn (PUTSTATIC, NAME_CL, "car", "I");
	}
	
	public static void actionChangeValue (MethodVisitor mv, int delta) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitInsn (DUP2);
		mv.visitInsn (IALOAD);
		mv.visitIntInsn (BIPUSH, delta);
		mv.visitInsn (IADD);
		mv.visitInsn (IASTORE);
	}
	
	public static void actionSetValue (MethodVisitor mv, int value) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitIntInsn (BIPUSH, value);
		mv.visitInsn (IASTORE);
	}
	
	public static void actionPrintValue (MethodVisitor mv) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_SYS, "out", 
							"L" + NAME_PRS + ";");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitInsn (IALOAD);
		mv.visitMethodInsn (INVOKEVIRTUAL, NAME_PRS, "println", 
							"(I)V", false);
	}
	
	public static void actionReadValue (MethodVisitor mv) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "S", 
							"L" + NAME_SC + ";");
		mv.visitMethodInsn (INVOKEVIRTUAL, NAME_SC, "nextInt", 
							"()I", false);
		mv.visitInsn (IASTORE);
	}
	
	private static class Compiler {
		
		private final String CODE;
		private int car = 0;
		
		public Compiler (String code) {
			if (code == null || code.length () == 0) {
				String message = "Code for compiling can't be empty";
				throw new IllegalArgumentException (message);
			}
			
			StringBuilder compressedString = new StringBuilder ();
			code.chars ()
				.filter (c -> !Character.isWhitespace (c))
				.forEach (c -> compressedString.append ((char) c));
			this.CODE = compressedString.toString ();
		}
		
		public int [] bounds () {
			int [] bounds = {0, 0};
			for (int i = 0, balance = 0; i < CODE.length (); i++) {
				if (CODE.charAt (i) == '>') {
					balance++;
				} else if (CODE.charAt (i) == '<') {
					balance--;
				}
				
				bounds [0] = Math.min (bounds [0], balance);
				bounds [1] = Math.max (bounds [1], balance);
			}
			
			return bounds;
		}
		
		public void compile (MethodVisitor mv) {
			parseExpression (mv);
		}
		
		private void parseExpression (MethodVisitor mv) {
			int collector = 0;
			
			while (car < CODE.length ()) {
				char sym = CODE.charAt (car);
				if (sym == '+' || sym == '-') {
					collector += sym == '+' ? 1 : -1;
				} else {
					if (Math.abs (collector) > 0) {
						actionChangeValue (mv, collector);
						collector = 0;
					}
					
					if (sym == '>') {
						actionMoveCarriage (mv, 1);
					} else if (sym == '<') {
						actionMoveCarriage (mv, -1);
					} else if (sym == '.') {
						actionPrintValue (mv);
					} else if (sym == ',') {
						actionReadValue (mv);
					} else if (sym == '[') {
						car++;
						parseCycle (mv);
					} else if (sym == ']') {
						return;
					}
				}
				
				car++;
			}
		}
		
		private void parseCycle (MethodVisitor mv) {
			Label l0 = new Label ();
			mv.visitLabel (l0);
			int tmpLine = LINE;
			mv.visitLineNumber (LINE++, l0);
			
			Label l1 = new Label ();
			mv.visitJumpInsn (GOTO, l1);
			
			Label l2 = new Label ();
			mv.visitLabel (l2);
			mv.visitLineNumber (LINE++, l2);
			mv.visitFrame (F_SAME, 0, null, 0, null);
			parseExpression (mv);
			
			mv.visitLabel (l1);
			mv.visitLineNumber (tmpLine, l1);
			mv.visitFrame (F_SAME, 0, null, 0, null);
			mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
			mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
			mv.visitInsn (IALOAD);
			mv.visitJumpInsn (IFGT, l2);
		}
		
	}
	
}
