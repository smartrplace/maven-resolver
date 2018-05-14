package org.smartrplace.drivers.maven.resolver.impl.checksums;

import java.io.InputStream;

public interface ChecksumValidation {
	
	/**
	 * Algorithm id, such as "sha1"; must correspond to the file ending of the checksum files.
	 * @return
	 */
	String algorithm();
	boolean validate(byte[] bytes, String expectedResultHex);
	boolean validate(InputStream stream, String expectedResultHex);
	
}
