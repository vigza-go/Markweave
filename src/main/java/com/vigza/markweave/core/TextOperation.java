package com.vigza.markweave.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

public class TextOperation {
    private List<Object> ops;
    private int baseLength;
    private int targetLength;

    public TextOperation() {
        this.ops = new ArrayList<>();
        this.baseLength = 0;
        this.targetLength = 0;
    }

    // --- 类型判断辅助方法 ---

    public static boolean isRetain(Object op) {
        return op instanceof Integer && (Integer) op > 0;
    }

    public static boolean isInsert(Object op) {
        return op instanceof String;
    }

    public static boolean isDelete(Object op) {
        return op instanceof Integer && (Integer) op < 0;
    }

    // --- Builder 方法 ---

    public TextOperation retain(int n) {
        if (n == 0) return this;
        this.baseLength += n;
        this.targetLength += n;
        if (!ops.isEmpty() && isRetain(ops.get(ops.size() - 1))) {
            ops.set(ops.size() - 1, (Integer) ops.get(ops.size() - 1) + n);
        } else {
            ops.add(n);
        }
        return this;
    }

    public TextOperation insert(String str) {
        if (str == null || str.isEmpty()) return this;
        this.targetLength += str.length();
        if (!ops.isEmpty() && isInsert(ops.get(ops.size() - 1))) {
            ops.set(ops.size() - 1, (String) ops.get(ops.size() - 1) + str);
        } else if (!ops.isEmpty() && isDelete(ops.get(ops.size() - 1))) {
            // 规范化：Insert 永远在 Delete 之前
            Object lastOp = ops.remove(ops.size() - 1);
            if (!ops.isEmpty() && isInsert(ops.get(ops.size() - 1))) {
                ops.set(ops.size() - 1, (String) ops.get(ops.size() - 1) + str);
            } else {
                ops.add(str);
            }
            ops.add(lastOp);
        } else {
            ops.add(str);
        }
        return this;
    }

    public TextOperation delete(int n) {
        if (n == 0) return this;
        if (n > 0) n = -n;
        this.baseLength -= n;
        if (!ops.isEmpty() && isDelete(ops.get(ops.size() - 1))) {
            ops.set(ops.size() - 1, (Integer) ops.get(ops.size() - 1) + n);
        } else {
            ops.add(n);
        }
        return this;
    }

    // --- 核心算法：Transform ---

    public static TextOperation[] transform(TextOperation op1, TextOperation op2) {
        if (op1.baseLength != op2.baseLength) {
            throw new RuntimeException("Base lengths must be equal");
        }

        TextOperation op1Prime = new TextOperation();
        TextOperation op2Prime = new TextOperation();
        
        List<Object> ops1 = new ArrayList<>(op1.ops);
        List<Object> ops2 = new ArrayList<>(op2.ops);
        
        int i1 = 0, i2 = 0;
        
        // 模拟 JS 的 shift() 逻辑
        Object o1 = null, o2 = null;

        while (i1 < ops1.size() || i2 < ops2.size() || o1 != null || o2 != null) {
            if (o1 == null && i1 < ops1.size()) o1 = ops1.get(i1++);
            if (o2 == null && i2 < ops2.size()) o2 = ops2.get(i2++);

            // Case: Insert
            if (isInsert(o1)) {
                op1Prime.insert((String) o1);
                op2Prime.retain(((String) o1).length());
                o1 = null; continue;
            }
            if (isInsert(o2)) {
                op1Prime.retain(((String) o2).length());
                op2Prime.insert((String) o2);
                o2 = null; continue;
            }

            if (o1 == null || o2 == null) break;

            // Case: Retain/Retain, Delete/Delete, Retain/Delete...
            if (isRetain(o1) && isRetain(o2)) {
                int r1 = (Integer) o1, r2 = (Integer) o2;
                if (r1 > r2) {
                    op1Prime.retain(r2); op2Prime.retain(r2);
                    o1 = r1 - r2; o2 = null;
                } else if (r1 == r2) {
                    op1Prime.retain(r1); op2Prime.retain(r1);
                    o1 = null; o2 = null;
                } else {
                    op1Prime.retain(r1); op2Prime.retain(r1);
                    o2 = r2 - r1; o1 = null;
                }
            } else if (isDelete(o1) && isDelete(o2)) {
                int d1 = -(Integer) o1, d2 = -(Integer) o2;
                if (d1 > d2) {
                    o1 = -(d1 - d2); o2 = null;
                } else if (d1 == d2) {
                    o1 = null; o2 = null;
                } else {
                    o2 = -(d2 - d1); o1 = null;
                }
            } else if (isDelete(o1) && isRetain(o2)) {
                int d1 = -(Integer) o1, r2 = (Integer) o2;
                if (d1 > r2) {
                    op1Prime.delete(r2);
                    o1 = -(d1 - r2); o2 = null;
                } else if (d1 == r2) {
                    op1Prime.delete(r2);
                    o1 = null; o2 = null;
                } else {
                    op1Prime.delete(d1);
                    o2 = r2 - d1; o1 = null;
                }
            } else if (isRetain(o1) && isDelete(o2)) {
                int r1 = (Integer) o1, d2 = -(Integer) o2;
                if (r1 > d2) {
                    op2Prime.delete(d2);
                    o1 = r1 - d2; o2 = null;
                } else if (r1 == d2) {
                    op2Prime.delete(r1);
                    o1 = null; o2 = null;
                } else {
                    op2Prime.delete(r1);
                    o2 = -(d2 - r1); o1 = null;
                }
            }
        }
        return new TextOperation[]{op1Prime, op2Prime};
    }

    // --- 应用到字符串 ---

    public String apply(String str) {
        if (str.length() != baseLength) {
            throw new RuntimeException("The operation's base length must be equal to the string's length.");
        }
        StringBuilder sb = new StringBuilder();
        int strIndex = 0;
        for (Object op : ops) {
            if (isRetain(op)) {
                int n = (Integer) op;
                sb.append(str, strIndex, strIndex + n);
                strIndex += n;
            } else if (isInsert(op)) {
                sb.append((String) op);
            } else if (isDelete(op)) {
                strIndex -= (Integer) op;
            }
        }
        return sb.toString();
    }

    // --- Getter & Utils ---
    public List<Object> getOps() { return ops; }
    public int getBaseLength() { return baseLength; }
    public int getTargetLength() { return targetLength; }

    @Override
    public String toString() { return ops.toString(); }

    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object op : ops) {
            if (isRetain(op)) {
                sb.append((Integer) op);
            } else if (isInsert(op)) {
                sb.append("\"").append((String) op).append("\"");
            } else if (isDelete(op)) {
                sb.append((Integer) op);
            }
            sb.append(",");
        }
        sb.setCharAt(sb.length() - 1, ']');
        return sb.toString();
    }

    public TextOperation fromJSON(String json){
        JSONArray jsArr = JSONUtil.parseArray(json);
        for (Object op : jsArr) {
            if (isRetain(op)) {
                retain((Integer) op);
            } else if (isInsert(op)) {
                insert((String) op);
            } else if (isDelete(op)) {
                delete((Integer) op);
            }
        }
        return this;
    }
}