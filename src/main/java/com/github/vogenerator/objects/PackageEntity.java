package com.github.vogenerator.objects;

import java.util.ArrayList;
import java.util.List;

public class PackageEntity {

	private final List<PrototypeEntity> entities = new ArrayList<PrototypeEntity>();
	private String identifier;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public List<PrototypeEntity> getEntities() {
		return entities;
	}

	@Override
	public String toString() {
		return "PackageEntity [entities=" + entities + ", identifier=" + identifier + "]";
	}

}
