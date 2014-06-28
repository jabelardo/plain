package com.ibm.plain.io;

import java.io.OutputStream;

public final class DevNull {

	public static OutputStream devnull = com.ibm.plain.io.NullOutputStream$.MODULE$
			.getInstance();

}
