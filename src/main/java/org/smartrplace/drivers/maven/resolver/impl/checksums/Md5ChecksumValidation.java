package org.smartrplace.drivers.maven.resolver.impl.checksums;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.smartrplace.drivers.maven.resolver.impl.MavenResolver;

class Md5ChecksumValidation implements ChecksumValidation {

	@Override
	public String algorithm() {
		return "md5";
	}

	@Override
	public boolean validate(byte[] bytes, String expectedResult) {
		return DigestUtils.md5Hex(bytes).equals(expectedResult);
	}

	@Override
	public boolean validate(InputStream stream, String expectedResult) {
		if (expectedResult == null)
			return false;
		try {
			return DigestUtils.md5Hex(stream).equals(expectedResult);
		} catch (IOException e) {
			MavenResolver.warn("Failed to compute checksum",e);
			return false;
		}
	}

}
