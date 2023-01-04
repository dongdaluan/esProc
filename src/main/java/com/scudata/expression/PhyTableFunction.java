package com.scudata.expression;

import com.scudata.dw.IPhyTable;

/**
 * 组表成员函数基类
 * @author RunQian
 *
 */
public abstract class PhyTableFunction extends MemberFunction {
	protected IPhyTable table;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof IPhyTable;
	}

	public void setDotLeftObject(Object obj) {
		table = (IPhyTable)obj;
	}
}