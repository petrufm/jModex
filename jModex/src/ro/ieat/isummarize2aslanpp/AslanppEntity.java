/*
 * Copyright (c) 2013-2014 Institute eAustria Timisoara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ro.ieat.isummarize2aslanpp;

import java.util.ArrayList;

class AslanppEntity extends AslanppMetaEntity {

	private String name;
	private ArrayList<AslanppEntity> entities = new ArrayList<AslanppEntity>();
	private ArrayList<AslanppSymbol> symbols = new ArrayList<AslanppSymbol>();
	private ArrayList<AslanppParameter> parameters = new ArrayList<AslanppParameter>();
	private ArrayList<AslanppStatement> body = new ArrayList<AslanppStatement>();
	private ArrayList<AslanppType> types = new ArrayList<AslanppType>();
	private ArrayList<AslanppStringStatement> clauses = new ArrayList<AslanppStringStatement>();
	
	AslanppEntity(String name, AslanppMetaEntity parent) {
		super(parent);
		this.name = name;
	}
	
	@Override
	public void print(int tabs, StringBuilder str) {
		super.print(tabs, str);
		str.append("entity " + name);
		if(!parameters.isEmpty()) {
			str.append("(");
			for(AslanppParameter aPar : parameters) {
				aPar.print(tabs,str); 
				str.append(",");
			}
			str.deleteCharAt(str.length()-1);
			str.append(")");
		}
		str.append(" {\n");
		if(!types.isEmpty()) {
			super.print(tabs + 1, str);
			str.append("types\n");
			for(AslanppType aType : types) {
				aType.print(tabs + 2, str);
			}
		}
		if(!symbols.isEmpty()) {
			super.print(tabs + 1, str);
			str.append("symbols\n");
			for(AslanppSymbol aSymbol : symbols) {
				aSymbol.print(tabs + 2, str);
			}
		}
		if(!clauses.isEmpty()) {
			super.print(tabs + 1, str);
			str.append("clauses\n");
			for(AslanppStringStatement aClause : clauses) {
				aClause.print(tabs + 2, str);
			}
		}
		for(AslanppEntity anEntity : entities) {
			anEntity.print(tabs + 1, str);
		}
		super.print(tabs + 1, str);		
		str.append("body {\n");
		for(AslanppStatement stm : body) {
			stm.print(tabs + 2, str);
		}
		super.print(tabs + 1, str);		
		str.append("}\n");
		super.print(tabs, str);
		str.append("}\n");
	}
	
	void addStatement(AslanppStatement stm) {
		body.add(stm);
	}

	AslanppEntity searchOrAddSubEntity(String name) {
		for(AslanppEntity anEntity : entities) {
			if(anEntity.name.equals(name)) {
				return anEntity;
			}
		}
		AslanppEntity tmp = new AslanppEntity(name,this);
		entities.add(tmp);
		return tmp;
	}

	String getName() {
		return name;
	}

	AslanppParameter searchOrAddParameter(String name, String structure, AslanppType type) {
		for(AslanppParameter aPar : parameters) {
			if(aPar.getName().equals(name)) {
				return aPar;
			}
		}
		AslanppParameter aPar = new AslanppParameter(name,structure,type,this);
		parameters.add(aPar);
		return aPar;
	}

	AslanppSymbol searchOrAddSymbol(boolean isConstant, boolean isLocal, String name, String structure, AslanppType type) {
		if(isConstant) {
			name = new Character(name.charAt(0)).toString().toLowerCase() + name.substring(1);
		} else {
			name = new Character(name.charAt(0)).toString().toUpperCase() + name.substring(1);			
		}
		for(AslanppSymbol aSymb : symbols) {
			if(aSymb.getName().equals(name)) {
				return aSymb;
			}
		}
		if(!isLocal && parent != null && parent instanceof AslanppEntity) {
			AslanppSymbol aSymb = ((AslanppEntity)parent).searchOrAddSymbol(isConstant, false, name, structure, type);
			if(aSymb != null) {
				return aSymb;
			}
		}
		AslanppSymbol aSymb = new AslanppSymbol(isConstant, name,structure,type,this);
		symbols.add(aSymb);
		return aSymb;
	}

	AslanppType searchOrAddType(String typeName, AslanppType superType, boolean isLocal) {
		if(typeName.equals("message")) {return AslanppType.messageType;}
		if(typeName.equals("text")) {return AslanppType.textType;}
		if(typeName.equals("nat")) {return AslanppType.natType;}
		if(typeName.equals("fact")) {return AslanppType.factType;}
		if(typeName.equals("agent")) {return AslanppType.agentType;}
		for(int i = 0; i < types.size(); i++) {
			if(types.get(i).getName().equals(typeName)) {
				return types.get(i);
			}
		}
		if(!isLocal && parent != null && parent instanceof AslanppEntity) {
			AslanppType aType = ((AslanppEntity)parent).searchOrAddType(typeName, superType, isLocal);
			if(aType != null) {
				return aType;
			}
		}
		AslanppType aType = new AslanppType(typeName,superType);
		types.add(aType);
		return aType;
	}
	
	void addClause(AslanppStringStatement clause) {
		for(AslanppStringStatement s : clauses) {
			if(s.isTheSame((AslanppStatement)clause)) {
				return;
			}
		}
		clauses.add(clause);
	}
}
