package org.smartrplace.drivers.maven.resolver.impl.checksums;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.smartrplace.drivers.maven.resolver.impl.MavenResolver;

class Sha1ChecksumValidation implements ChecksumValidation {

	@Override
	public String algorithm() {
		return "sha1";
	}

	@Override
	public boolean validate(byte[] bytes, String expectedResult) {
		return DigestUtils.sha1Hex(bytes).equals(expectedResult);
	}

	@Override
	public boolean validate(InputStream stream, String expectedResult) {
		if (expectedResult == null)
			return false;
		try {
			return DigestUtils.sha1Hex(stream).equals(expectedResult);
		} catch (IOException e) {
			MavenResolver.warn("Failed to compute checksum",e);
			return false;
		}
	}

}
