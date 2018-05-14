package org.smartrplace.drivers.maven.resolver.impl.checksums;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Checksums {

	private final static Map<String, ChecksumValidation> algorithms;
	
	static {
		final Map<String, ChecksumValidation> algos = new HashMap<>(4);
		algos.put("sha1", new Sha1ChecksumValidation());
		algos.put("md5", new Md5ChecksumValidation());
		algorithms = Collections.unmodifiableMap(algos);
	}
	
	public static ChecksumValidation getValidator(String algo) {
		return algorithms.get(algo);
	}
	
	public static Collection<String> getAlgos() {
		return algorithms.keySet();
	}
	
}
