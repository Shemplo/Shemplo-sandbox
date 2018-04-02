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
								NAME_STA = name (String [].class),
								NAME_I   = name (Integer.class),
								NAME_OBJ = name (Object.class);
	
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
			// If file with given path exists then source code
			// will be read from this file
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
				System.exit (1);
			}
		}
		
		/* Initializing compiler on given code */
		Compiler c = new Compiler (code);
		
		/* Creating class with name `Brainfuck` */
		/* Equals: public class Brainfuck {} */
		cw = new ClassWriter (0);
		int CLASS_ACCESS = ACC_PUBLIC + ACC_SUPER;
		cw.visit (V1_8, CLASS_ACCESS, NAME_CL, 
					null, "java/lang/Object", null);
		cw.visitSource (NAME_CL + ".java", null);
		
		/*
		 * Declaring variables:
		 * private static final Scanner S;
		 * private static int [] tape;
		 * private static int car;
		 */
		declareVaribles ();
		/*
		 * Assign variables:
		 * S = new Scanner (System.in, StandardCharsets.UTF_8)
		 * tape = new int [size];
		 * car = offset;
		 */
		int size = 4; // Size of tape (by default)
		declareStaticConstructor (size, size / 2);
		
		MethodVisitor mv = null;
		/*
		 * Creating `main` method of class
		 * Equals: public static void main (String... args) {}
		 */
		int MAIN_ACCESS = ACC_PUBLIC + ACC_STATIC + ACC_VARARGS;
		mv = cw.visitMethod (MAIN_ACCESS, "main", "(" + NAME_STA + ")V", 
								null, null);
		mv.visitCode ();
		
		/* Add body of `main` method */
		// This is a compilation of BF code
		c.compile (mv);
		
		// Some meta information for method
		mv.visitInsn (RETURN);
		mv.visitMaxs (10, 1);
		mv.visitEnd ();
		
		// Support method that will expand array of tape
		// when it's necessary and possible
		declareExpandMethod ();
		
		// Special wrapper that will make expand
		// before moving carriage
		declareMoveMethod ();
		
		// Closing class for editing
		cw.visitEnd ();
		
		// Creating a file to write class in
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
	
	/**
	 * @param token Type-token of class that's name is required
	 * @return Name of object in format package1/package2/.../ClassName
	 */
	private static String name (Class <?> token) {
		if (token == null) { return "null"; }
		return token.getName ().replace ('.', '/');
	}
	
	/**
	 * Declares necessary variables (Scanner + Array + Carriage)
	 */
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
	
	/**
	 * @param tapeLength length of tape
	 * @param offset start possition of carriage
	 * 
	 */
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
	
	private static void declareExpandMethod () {
		int ACC = ACC_PRIVATE + ACC_STATIC;
		MethodVisitor mv = cw.visitMethod (ACC, "expand", "(I)V", 
											null, null);
		mv.visitParameter ("offset", 0);
		mv.visitCode ();
		
		Label la = new Label ();
		mv.visitLabel (la);
		mv.visitLineNumber (LINE++, la);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitInsn (ARRAYLENGTH);
		mv.visitFieldInsn (GETSTATIC, NAME_I, "MAX_VALUE", "I");
		
		// In case of max capacity of tape no reasons to continue
		// if (tape.length == Integer.MAX_VALUE)
		Label lb = new Label ();
		mv.visitJumpInsn (IF_ICMPNE, lb);
		
			// return;
			Label lc = new Label ();
			mv.visitLabel (lc);
			mv.visitLineNumber (LINE++, lc);
			mv.visitInsn (RETURN);
		
		// Exit from `if`
		mv.visitLabel (lb);
		mv.visitLineNumber (LINE++, lb);
		mv.visitFrame (F_SAME, 0, null, 0, null);
		
		// Evaluating expected position of carriage
		// int shift = car + offset;
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitVarInsn (ILOAD, 0);
		mv.visitInsn (IADD);
		mv.visitVarInsn (ISTORE, 1);
		
		// Storing default size of new array
		// int len = Integer.MAX_VALUE;
		Label l01 = new Label ();
		mv.visitLabel (l01);
		mv.visitLineNumber (LINE++, l01);
		mv.visitFieldInsn (GETSTATIC, NAME_I, "MAX_VALUE", "I");
		mv.visitVarInsn (ISTORE, 2);
		
		Label l1 = new Label ();
		mv.visitLabel (l1);
		mv.visitLineNumber (LINE++, l1);
		mv.visitVarInsn (ILOAD, 1);
		
		// Check that expand is necessary
		// if (shift < 0)
		Label l2 = new Label ();
		mv.visitJumpInsn (IFLT, l2);
		mv.visitVarInsn (ILOAD, 1);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitInsn (ARRAYLENGTH);
		
			// Check that expand is necessary
			// if (shift >= tape.length)
			Label l3 = new Label ();
			mv.visitJumpInsn (IF_ICMPLT, l3);
			
			mv.visitLabel (l2);
			mv.visitLineNumber (LINE++, l2);
			mv.visitFrame (F_APPEND, 2, new Object [] {INTEGER, INTEGER}, 
							0, null);
			
			Label l4 = new Label ();
			mv.visitLabel (l4);
			mv.visitLineNumber (LINE++, l4);
			mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
			mv.visitInsn (ARRAYLENGTH);
			mv.visitFieldInsn (GETSTATIC, NAME_I, "MAX_VALUE", "I");
			mv.visitInsn (ICONST_2);
			mv.visitInsn (IDIV);
			
			// Check that we can increase in 2 times
			// if (tape.length < Integer.MAX_VALUE / 2)
			Label l5 = new Label ();
			mv.visitJumpInsn (IF_ICMPGE, l5);
			
				// Evaluating new size of tape
				// len = tape.length * 2;
				Label l6 = new Label ();
				mv.visitLabel (l6);
				mv.visitLineNumber (LINE++, l6);
				mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
				mv.visitInsn (ARRAYLENGTH);
				mv.visitInsn (ICONST_2);
				mv.visitInsn (IMUL);
				mv.visitVarInsn (ISTORE, 2);
				
			// Exit from `if`
			mv.visitLabel (l5);
			mv.visitLineNumber (LINE++, l5);
			mv.visitFrame (F_SAME, 0, null, 0, null);
			
			// Creating temporary array for tape
			// int[] arrayOfInt = new int[len];
			mv.visitVarInsn (ILOAD, 2);
			mv.visitIntInsn (NEWARRAY, T_INT);
			mv.visitVarInsn (ASTORE, 4);
			
			// Copying existing array to new corpus with 1 quarter offset
			// (in this situation existing array will be placed in center)
			// System.arraycopy(tape, 0, arrayOfInt, len / 4, tape.length);
			Label l7 = new Label ();
			mv.visitLabel (l7);
			mv.visitLineNumber (LINE++, l7);
			mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
			mv.visitInsn (ICONST_0);
			mv.visitVarInsn (ALOAD, 4);
			mv.visitVarInsn (ILOAD, 2);
			mv.visitInsn (ICONST_4);
			mv.visitInsn (IDIV);
			mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
			mv.visitInsn (ARRAYLENGTH);
			String type = "(L" + NAME_OBJ + ";IL" + NAME_OBJ + ";II)V";
			mv.visitMethodInsn (INVOKESTATIC, NAME_SYS, "arraycopy", 
								type, false);
			
			// Assign temporary array as array of tape
			// tape = arrayOfInt;
			Label l8 = new Label ();
			mv.visitLabel (l8);
			mv.visitLineNumber (LINE++, l8);
			mv.visitVarInsn (ALOAD, 4);
			mv.visitFieldInsn (PUTSTATIC, NAME_CL, "tape", "[I");
			
			// Moving carriage on 1 quarter due to centering
			// of existing array in new one
			// car += len / 4;
			Label l9 = new Label ();
			mv.visitLabel (l9);
			mv.visitLineNumber (LINE++, l9);
			mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
			mv.visitVarInsn (ILOAD, 2);
			mv.visitInsn (ICONST_4);
			mv.visitInsn (IDIV);
			mv.visitInsn (IADD);
			mv.visitFieldInsn (PUTSTATIC, NAME_CL, "car", "I");
		
		// Exit from `if`
		mv.visitLabel (l3);
		mv.visitLineNumber (LINE++, l3);
		mv.visitFrame (F_SAME, 0, null, 0, null);
		
		mv.visitInsn (RETURN);
		mv.visitLocalVariable ("offset", "I", null, la, l7, 0);
		mv.visitLocalVariable ("shift", "I", null, la, l7, 1);
		mv.visitLocalVariable ("len", "I", null, la, l7, 2);
		mv.visitMaxs (10, 5);
		mv.visitEnd ();
	}
	
	private static void declareMoveMethod () {
		int ACC = ACC_PRIVATE + ACC_STATIC;
		MethodVisitor mv = cw.visitMethod (ACC, "move", "(I)V", 
											null, null);
		mv.visitParameter ("offset", 0);
		mv.visitCode ();
		
		// Ensuring capacity of tape
		// expand(offset);
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitVarInsn (ILOAD, 0);
		mv.visitMethodInsn (INVOKESTATIC, NAME_CL, "expand", 
							"(I)V", false);
		
		// Moving carriage by given offset
		// car = (int)((car + tape.length + offset) % tape.length);
		Label l1 = new Label ();
		mv.visitLabel (l1);
		mv.visitLineNumber (LINE++, l1);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitInsn (I2L);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitInsn (ARRAYLENGTH);
		mv.visitInsn (I2L);
		mv.visitInsn (LADD);
		mv.visitVarInsn (ILOAD, 0);
		mv.visitInsn (I2L);
		mv.visitInsn (LADD);
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitInsn (ARRAYLENGTH);
		mv.visitInsn (I2L);
		mv.visitInsn (LREM);
		mv.visitInsn (L2I);
		mv.visitFieldInsn (PUTSTATIC, NAME_CL, "car", "I");
		
		mv.visitInsn (RETURN);
		mv.visitLocalVariable ("offset", "I", null, l0, l1, 0);
		mv.visitMaxs (10, 1);
		mv.visitEnd ();
	}
	
	public static void actionMoveCarriage (MethodVisitor mv, int offset) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitIntInsn (BIPUSH, offset);
		mv.visitMethodInsn (INVOKESTATIC, NAME_CL, "move", "(I)V", false);
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
	
	public static void actionPrintCharValue (MethodVisitor mv) {
		Label l0 = new Label ();
		mv.visitLabel (l0);
		mv.visitLineNumber (LINE++, l0);
		mv.visitFieldInsn (GETSTATIC, NAME_SYS, "out", 
							"L" + NAME_PRS + ";");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "tape", "[I");
		mv.visitFieldInsn (GETSTATIC, NAME_CL, "car", "I");
		mv.visitInsn (IALOAD);
		mv.visitInsn (I2C);
		mv.visitMethodInsn (INVOKEVIRTUAL, NAME_PRS, "print", 
							"(C)V", false);
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
		
		public void compile (MethodVisitor mv) {
			parseExpression (mv);
		}
		
		private void parseExpression (MethodVisitor mv) {
			int valueCollector = 0,
				moveCollector = 0;
			
			while (car < CODE.length ()) {
				char sym = CODE.charAt (car);
				if (sym == '+' || sym == '-') {
					valueCollector += sym == '+' ? 1 : -1;
					if (Math.abs (moveCollector) > 0) {
						actionMoveCarriage (mv, moveCollector);
						moveCollector = 0;
					}
				} else if (sym == '>' || sym == '<') {
					moveCollector += sym == '>' ? 1 : -1;
					if (Math.abs (valueCollector) > 0) {
						actionChangeValue (mv, valueCollector);
						valueCollector = 0;
					}
				} else {
					if (Math.abs (valueCollector) > 0) {
						actionChangeValue (mv, valueCollector);
						valueCollector = 0;
					}
					
					if (Math.abs (moveCollector) > 0) {
						actionMoveCarriage (mv, moveCollector);
						moveCollector = 0;
					}
					
					if (sym == '.') {
						actionPrintValue (mv);
					} else if (sym == ':') {
						actionPrintCharValue (mv);
					} else if (sym == ',') {
						actionReadValue (mv);
					} else if (sym == '[') {
						car++;
						parseCycle (mv);
					} else if (sym == ']') {
						return;
					} else {
						String message = "Unknown symbol `" + sym + "` at " + (car + 1);
						throw new IllegalStateException (message);
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
