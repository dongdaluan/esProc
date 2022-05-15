package com.scudata.dw;

/**
 * ������������
 * @author runqian
 *
 */
public class LogicAnd extends IFilter {
	private IFilter left;
	private IFilter right;
	
	public LogicAnd(IFilter left, IFilter right) {
		super(left.column, left.priority);
		this.left = left;
		this.right = right;
	}

	public LogicAnd(IFilter left, IFilter right, String columnName) {
		this.columnName = columnName;
		priority = left.priority;
		this.left = left;
		this.right = right;
	}

	public IFilter getLeft() {
		return left;
	}

	public IFilter getRight() {
		return right;
	}

	public boolean match(Object value) {
		return left.match(value) && right.match(value);
	}
	
	public boolean match(Object minValue, Object maxValue) {
		return left.match(minValue, maxValue) && right.match(minValue, maxValue);
	}
}