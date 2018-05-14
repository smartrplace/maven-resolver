package org.smartrplace.drivers.maven.resolver.impl;

import java.io.InputStream;
import java.util.Objects;

class ResolutionResult implements AutoCloseable {

	final InputStream result;
	// may be null
	final String checksumAlgo;
	// null if checksumAlgo is null
	final InputStream checksumInput;
	
	ResolutionResult(InputStream result) {
		this(result, null, null);
	}
	
	ResolutionResult(InputStream result, String checksumAlgo, InputStream checksumInput) {
		this.result = Objects.requireNonNull(result);
		this.checksumAlgo = checksumAlgo;
		this.checksumInput = checksumInput;
	}
	
	@Override
	public void close() {
		Client.closeSmoothly(result);
		Client.closeSmoothly(checksumInput);
	}
	
}
