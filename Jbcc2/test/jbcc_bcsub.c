/* Based on robovm/compiler/src/main/resources/header.ll
 */
#include "jbcc.h"

Int jbcc_idiv(Int op1, Int op2) {
	if (op2 != 0) {
		if (op2 != -1) return op1 / op2;
		return op1 * op2;
	}
	jbcc_throwArithmeticException();
}

Long jbcc_ldiv(Long op1, Long op2) {
	if (op2 != 0) {
		if (op2 != -1) return op1 / op2;
		return op1 * op2;
	}
	jbcc_throwArithmeticException();
}

Int jbcc_irem(Int op1, Int op2) {
	if (op2 != 0) {
		if (op2 != -1) return op1 % op2;
		return 0;
	}
	jbcc_throwArithmeticException();
}

Long jbcc_lrem(Long op1, Long op2) {
	if (op2 != 0) {
		if (op2 != -1) return op1 % op2;
		return 0L;
	}
	jbcc_throwArithmeticException();
}

Int jbcc_d2i(Double op) {
    if (op != op) return 0L;		// %op == NaN
    if (op >= Int_MAX) return Int_MAX;
    if (op <= Int_MIN) return Int_MIN;
    return (Int) op;
}

Long jbcc_d2l(Double op) {
    if (op != op) return 0L;		// %op == NaN
    if (op >= Long_MAX) return Long_MAX;
    if (op <= Long_MIN) return Long_MIN;
    return (Long) op;
}

Int jbcc_f2i(Float op) {
    if (op != op) return 0;		// %op == NaN
    if (op >= Int_MAX) return Int_MAX;
    if (op <= Int_MIN) return Int_MIN;
    return (Int) op;
}

Long jbcc_f2l(Float op) {
    if (op != op) return 0L;		// %op == NaN
    if (op >= Long_MAX) return Long_MAX;
    if (op <= Long_MIN) return Long_MIN;
    return (Long) op;
}

LabelPtr jbcc_lookupswitch(Int value, int length, SwitchPair table[], LabelPtr defalt) {
	// TODO use a binary search, since table is sorted
	for (int i = 0;  i < length;  i++) {
		if (table[i].v == value) return table[i].l;
	}
	return defalt;
}

/*

define private i1 @isinstance_class(%Object* %o, i32 %offset, i32 %id) alwaysinline {
    %c = call %Class* @Object_class(%Object* %o)
    %ti = call %TypeInfo* @Class_typeInfo(%Class* %c)
    %cachedId = call i32 @TypeInfo_cache(%TypeInfo* %ti)
    %isCachedEQ = icmp eq i32 %id, %cachedId
    br i1 %isCachedEQ, label %found, label %notInCache
notInCache:
    %otherOffset = call i32 @TypeInfo_offset(%TypeInfo* %ti)
    %isOffsetLE = icmp ule i32 %offset, %otherOffset
    br i1 %isOffsetLE, label %compareIds, label %notFound
compareIds:
    %1 = bitcast %TypeInfo* %ti to [0 x i8]*
    %2 = getelementptr [0 x i8]* %1, i32 0, i32 %offset
    %3 = bitcast i8* %2 to i32*
    %otherId = load volatile i32* %3
    %isIdEQ = icmp eq i32 %id, %otherId
    br i1 %isIdEQ, label %storeCache, label %notFound
storeCache:
    call void @TypeInfo_cache_store(%TypeInfo* %ti, i32 %id)
    br label %found
found:
    ret i1 1
notFound:
    ret i1 0
}

define private i1 @isinstance_interface(%Object* %o, i32 %id) alwaysinline {
    %c = call %Class* @Object_class(%Object* %o)
    %ti = call %TypeInfo* @Class_typeInfo(%Class* %c)
    %cachedId = call i32 @TypeInfo_cache(%TypeInfo* %ti)
    %isCachedEQ = icmp eq i32 %id, %cachedId
    br i1 %isCachedEQ, label %found, label %notInCache
notInCache:
    %ifCount = call i32 @TypeInfo_interfaceCount(%TypeInfo* %ti)
    %hasIfs = icmp ne i32 %ifCount, 0
    br i1 %hasIfs, label %computeBase, label %notFound
computeBase:
    %offset = call i32 @TypeInfo_offset(%TypeInfo* %ti)
    %ti_18p = bitcast %TypeInfo* %ti to [0 x i8]*
    %1 = getelementptr [0 x i8]* %ti_18p, i32 0, i32 %offset
    %2 = bitcast i8* %1 to [0 x i32]*
    %3 = getelementptr [0 x i32]* %2, i32 0, i32 1
    %base = bitcast i32* %3 to [0 x i32]* ; %base now points to the first interface id
    br label %loop
loop:
    %n_phi = phi i32 [0, %computeBase], [%n, %checkDone]
    %4 = getelementptr [0 x i32]* %base, i32 0, i32 %n_phi
    %n = add i32 %n_phi, 1
    %otherId = load volatile i32* %4
    %isIdEQ = icmp eq i32 %id, %otherId
    br i1 %isIdEQ, label %storeCache, label %checkDone
checkDone:
    %isDone = icmp eq i32 %n, %ifCount
    br i1 %isDone, label %notFound, label %loop
storeCache:
    call void @TypeInfo_cache_store(%TypeInfo* %ti, i32 %id)
    br label %found
found:
    ret i1 1
notFound:
    ret i1 0
}

define private %Object* @checkcast_class(%Env* %env, i8** %header, %Object* %o, i32 %offset, i32 %id) alwaysinline {
    %isNotNull = icmp ne %Object* %o, null
    br i1 %isNotNull, label %notNull, label %null
null:
    ret %Object* null
notNull:
    %isInstance = call i1 @isinstance_class(%Object* %o, i32 %offset, i32 %id)
    br i1 %isInstance, label %ok, label %throw
ok:
    ret %Object* %o
throw:
    call void @_bcThrowClassCastException(%Env* %env, i8** %header, %Object* %o)
    unreachable
}

define private %Object* @checkcast_interface(%Env* %env, i8** %header, %Object* %o, i32 %id) alwaysinline {
    %isNotNull = icmp ne %Object* %o, null
    br i1 %isNotNull, label %notNull, label %null
null:
    ret %Object* null
notNull:
    %isInstance = call i1 @isinstance_interface(%Object* %o, i32 %id)
    br i1 %isInstance, label %ok, label %throw
ok:
    ret %Object* %o
throw:
    call void @_bcThrowClassCastException(%Env* %env, i8** %header, %Object* %o)
    unreachable
}

define private i32 @instanceof_class(%Env* %env, i8** %header, %Object* %o, i32 %offset, i32 %id) alwaysinline {
    %isNotNull = icmp ne %Object* %o, null
    br i1 %isNotNull, label %notNull, label %false
notNull:
    %isInstance = call i1 @isinstance_class(%Object* %o, i32 %offset, i32 %id)
    br i1 %isInstance, label %true, label %false
true:
    ret i32 1
false:
    ret i32 0
}

define private i32 @instanceof_interface(%Env* %env, i8** %header, %Object* %o, i32 %id) alwaysinline {
    %isNotNull = icmp ne %Object* %o, null
    br i1 %isNotNull, label %notNull, label %false
notNull:
    %isInstance = call i1 @isinstance_interface(%Object* %o, i32 %id)
    br i1 %isInstance, label %true, label %false
true:
    ret i32 1
false:
    ret i32 0
}

define private %Object* @checkcast_prim_array(%Env* %env, %Class* %arrayClass, %Object* %o) alwaysinline {
    %isNotNull = icmp ne %Object* %o, null
    br i1 %isNotNull, label %notNull, label %dontThrow
notNull:
    %cls = call %Class* @Object_class(%Object* %o)
    %isSame = icmp eq %Class* %cls, %arrayClass
    br i1 %isSame, label %dontThrow, label %throw
dontThrow:
    ret %Object* %o;
throw:
    call void @_bcThrowClassCastExceptionArray(%Env* %env, %Class* %arrayClass, %Object* %o)
    unreachable
}

define private i32 @instanceof_prim_array(%Env* %env, %Class* %arrayClass, %Object* %o) alwaysinline {
    %isNotNull = icmp ne %Object* %o, null
    br i1 %isNotNull, label %notNull, label %false
notNull:
    %cls = call %Class* @Object_class(%Object* %o)
    %isSame = icmp eq %Class* %cls, %arrayClass
    br i1 %isSame, label %true, label %false
true:
    ret i32 1;
false:
    ret i32 0
}

define private void @monitorenter(%Env* %env, %Object* %o) alwaysinline {
    ; Try the common case first before we call _bcMonitorEnter
    %thin = call i32 @Object_lock(%Object* %o)
    %thinBit = and i32 %thin, 1
    %isThin = icmp eq i32 %thinBit, 0
    br i1 %isThin, label %yesThin, label %callBc
yesThin:
    %1 = lshr i32 %thin, 3 ; LW_LOCK_OWNER_SHIFT = 3
    %owner = and i32 %1, 65535 ; LW_LOCK_OWNER_MASK = 0xffff
    %isUnowned = icmp eq i32 %owner, 0
    br i1 %isUnowned, label %tryLock, label %callBc
tryLock:
    %currentThread = call %Thread* @Env_currentThread(%Env* %env)
    %threadId = call i32 @Thread_threadId(%Thread* %currentThread)
    %2 = shl i32 %threadId, 3 ; LW_LOCK_OWNER_SHIFT = 3
    %newThin = or i32 %thin, %2
    %lockPtr = call i32* @Object_lockPtr(%Object* %o)
    %isSuccess = call i1 @atomic_cas(i32 %thin, i32 %newThin, i32* %lockPtr)
    br i1 %isSuccess, label %success, label %callBc
success:
    ret void
callBc:
    tail call void @_bcMonitorEnter(%Env* %env, %Object* %o)
    ret void
}

define private void @monitorexit(%Env* %env, %Object* %o) alwaysinline {
    ; Try the common case first before we call _bcMonitorExit
    %thin = call i32 @Object_lock(%Object* %o)
    %thinBit = and i32 %thin, 1
    %isThin = icmp eq i32 %thinBit, 0
    br i1 %isThin, label %yesThin, label %callBc
yesThin:
    %1 = lshr i32 %thin, 3 ; LW_LOCK_OWNER_SHIFT = 3
    %owner = and i32 %1, 65535 ; LW_LOCK_OWNER_MASK = 0xffff
    %currentThread = call %Thread* @Env_currentThread(%Env* %env)
    %threadId = call i32 @Thread_threadId(%Thread* %currentThread)
    %isOwner = icmp eq i32 %owner, %threadId
    br i1 %isOwner, label %maybeUnlock, label %callBc
maybeUnlock:
    %2 = lshr i32 %thin, 19 ; LW_LOCK_COUNT_SHIFT = 19
    %count = and i32 %2, 8191 ; LW_LOCK_COUNT_MASK = 0x1fff
    %lockPtr = call i32* @Object_lockPtr(%Object* %o)
    %isZero = icmp eq i32 %count, 0
    br i1 %isZero, label %unlock, label %callBc
unlock:
    %newThin = and i32 %thin, 6 ; LW_HASH_STATE_MASK << LW_HASH_STATE_SHIFT (0x3 << 1)
    fence seq_cst
    store volatile i32 %newThin, i32* %lockPtr
    ret void
callBc:
    tail call void @_bcMonitorExit(%Env* %env, %Object* %o)
    ret void
}

define private void @pushNativeFrame(%Env* %env) alwaysinline {
    ; Create a fake StackFrame
    %sf = alloca %StackFrame
    %sf_prev = getelementptr %StackFrame* %sf, i32 0, i32 0
    %sf_returnAddress = getelementptr %StackFrame* %sf, i32 0, i32 1
    %prevStackFrame = call i8* @llvm.frameaddress(i32 0)
    %pc = call i8* @getpc()
    store volatile i8* %prevStackFrame, i8** %sf_prev
    store volatile i8* %pc, i8** %sf_returnAddress

    %prev_gw = call %GatewayFrame* @Env_gatewayFrames(%Env* %env)
    %prev_gw_i8p = bitcast %GatewayFrame* %prev_gw to i8*

    ; Create the GatewayFrame
    %gw = alloca %GatewayFrame
    %gw_prev = getelementptr %GatewayFrame* %gw, i32 0, i32 0
    %gw_frameAddress = getelementptr %GatewayFrame* %gw, i32 0, i32 1
    %gw_proxyMethod = getelementptr %GatewayFrame* %gw, i32 0, i32 2
    store volatile i8* %prev_gw_i8p, i8** %gw_prev
    %sf_i8p = bitcast %StackFrame* %sf to i8*
    store volatile i8* %sf_i8p, i8** %gw_frameAddress
    store volatile i8* null, i8** %gw_proxyMethod

    call void @Env_gatewayFrames_store(%Env* %env, %GatewayFrame* %gw)

    ret void
}

define private void @popNativeFrame(%Env* %env) alwaysinline {
    %curr_gw = call %GatewayFrame* @Env_gatewayFrames(%Env* %env)
    %curr_gw_prev = getelementptr %GatewayFrame* %curr_gw, i32 0, i32 0
    %prev_gw_i8p = load volatile i8** %curr_gw_prev
    %prev_gw = bitcast i8* %prev_gw_i8p to %GatewayFrame*
    call void @Env_gatewayFrames_store(%Env* %env, %GatewayFrame* %prev_gw)
    ret void
}
*/
