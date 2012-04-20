package com.github.vogenerator;

import java.util.HashMap;
import java.util.Map;

import com.github.vogenerator.objects.PrototypeEntity;
import com.github.vogenerator.objects.Type;

public class TypeManager {

	private final Map<String, Type> types = new HashMap<String, Type>();

	public void registerType(final String typeId, final String inLanguage, final boolean enumType) {
		types.put(typeId, new Type() {

			public String getTypeInLanguage() {
				return inLanguage;
			}

			public String getTypeId() {
				return typeId;
			}

			public boolean isEnumType() {
				return enumType;
			}

			public boolean isGeneric() {
				return false;
			}

			public String getGenericTypeId() {
				return null;
			}
		});
	}

	public Type findType(String typeId, PrototypeEntity entity) {
		if (typeId == null) {
			return null;
		}

		String baseTypeId = typeId;
		if (baseTypeId.contains("<")) {
			baseTypeId = baseTypeId.substring(0, baseTypeId.indexOf("<"));
		}

		final Type type = types.get(baseTypeId);
		if (type == null) {
			throw new IllegalStateException("Type " + typeId + " used in entity " + entity.getPackageName() + "."
					+ entity.getIdentifier() + " was not found");
		}

		if (!baseTypeId.equals(typeId)) {
			final String genericTypeId = typeId.substring(typeId.indexOf("<") + 1, typeId.length() - 1);
			return new Type() {

				public boolean isGeneric() {
					return true;
				}

				public boolean isEnumType() {
					return type.isEnumType();
				}

				public String getTypeInLanguage() {
					return type.getTypeInLanguage();
				}

				public String getTypeId() {
					return type.getTypeId();
				}

				public String getGenericTypeId() {
					return genericTypeId;
				}
			};
		}

		return type;
	}
}
