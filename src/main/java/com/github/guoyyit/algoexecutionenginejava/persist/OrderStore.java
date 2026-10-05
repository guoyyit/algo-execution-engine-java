package com.github.guoyyit.algoexecutionenginejava.persist;

import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.DeviationRecord;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;

import java.util.List;
import java.util.Optional;

public interface OrderStore {

    void saveParent(ParentOrder parent);

    Optional<ParentOrder> getParent(String parentId);

    List<ParentOrder> allParents();

    void saveChild(ChildOrder child);

    Optional<ChildOrder> getChild(String childId);

    List<ChildOrder> childrenOf(String parentId);

    void savePlan(ExecutionPlan plan);

    Optional<ExecutionPlan> latestPlan(String parentId);

    List<DeviationRecord> deviations(String parentId);

    void recordDeviation(String parentOrderId, DeviationRecord deviation);

    long nextSeq();
}
