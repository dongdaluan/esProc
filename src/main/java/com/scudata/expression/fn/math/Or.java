package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 求序列成员或多个参数的按位或,非数值成员被忽略
 * @author yanjing
 *
 */
public class Or extends Function {
	// 计算序列成员按位或
	private static Object or(Sequence seq) {
		int size = seq.length();
		if (size == 0) {
			return null;
		} else if (size == 1) {
			return seq.getMem(1);
		}
		
		Object obj = seq.getMem(1);
		boolean returnInt = true; // 是否全部都是Integer，如果是则返回Integer，否则返回Long
		long longValue = 0;
		BigInteger bi = null; // 如果含有BigInteger则返回BigInteger
		
		// 如果有成员类型是BigDecimal或BigInteger则使用BigInteger运算
		if (obj instanceof BigDecimal) {
			bi = ((BigDecimal)obj).toBigInteger();
		} else if (obj instanceof BigInteger) {
			bi = (BigInteger)obj;
		} else if (obj instanceof Number) {
			if (!(obj instanceof Integer)) {
				returnInt = false;
			}
			
			longValue = ((Number)obj).longValue();
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("or" + mm.getMessage("function.paramTypeError"));
		}
		
		for (int i = 2; i <= size; ++i) {
			obj = seq.getMem(i);
			if (bi != null) {
				if (obj instanceof Number) {
					BigInteger tmp = Variant.toBigInteger((Number)obj);
					bi = bi.or(tmp);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("or" + mm.getMessage("function.paramTypeError"));
				}
			} else if (obj instanceof BigDecimal) {
				bi = ((BigDecimal)obj).toBigInteger();
				bi = bi.or(BigInteger.valueOf(longValue));
			} else if (obj instanceof BigInteger) {
				bi = (BigInteger)obj;
				bi = bi.or(BigInteger.valueOf(longValue));
			} else if (obj instanceof Number) {
				if (!(obj instanceof Integer)) {
					returnInt = false;
				}
				
				longValue |= ((Number)obj).longValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("or" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		if (bi == null) {
			if (returnInt) {
				return (int)longValue;
			} else {
				return longValue;
			}
		} else {
			return new BigDecimal(bi);
		}
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("or" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return or((Sequence)obj);
			} else {
				return obj;
			}
		} else {
			int size = param.getSubSize();
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("or" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			boolean returnInt = true; // 是否全部都是Integer，如果是则返回Integer，否则返回Long
			long longValue = 0;
			BigInteger bi = null; // 如果含有BigInteger则返回BigInteger
			
			// 如果有成员类型是BigDecimal或BigInteger则使用BigInteger运算
			if (obj instanceof BigDecimal) {
				bi = ((BigDecimal)obj).toBigInteger();
			} else if (obj instanceof BigInteger) {
				bi = (BigInteger)obj;
			} else if (obj instanceof Number) {
				if (!(obj instanceof Integer)) {
					returnInt = false;
				}
				
				longValue = ((Number)obj).longValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("or" + mm.getMessage("function.paramTypeError"));
			}
			
			for (int i = 1; i < size; ++i) {
				sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("or" + mm.getMessage("function.invalidParam"));
				}
				
				obj = sub.getLeafExpression().calculate(ctx);
				if (bi != null) {
					if (obj instanceof Number) {
						BigInteger tmp = Variant.toBigInteger((Number)obj);
						bi = bi.or(tmp);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("or" + mm.getMessage("function.paramTypeError"));
					}
				} else if (obj instanceof BigDecimal) {
					bi = ((BigDecimal)obj).toBigInteger();
					bi = bi.or(BigInteger.valueOf(longValue));
				} else if (obj instanceof BigInteger) {
					bi = (BigInteger)obj;
					bi = bi.or(BigInteger.valueOf(longValue));
				} else if (obj instanceof Number) {
					if (!(obj instanceof Integer)) {
						returnInt = false;
					}
					
					longValue |= ((Number)obj).longValue();
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("or" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (bi == null) {
				if (returnInt) {
					return (int)longValue;
				} else {
					return longValue;
				}
			} else {
				return new BigDecimal(bi);
			}
		}
	}
}
