package com.github.guoyyit.algoexecutionenginejava.web;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayRunner;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayRequest;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayResult;
import com.github.guoyyit.algoexecutionenginejava.service.AlgoExecutionService;
import com.github.guoyyit.algoexecutionenginejava.service.OrderServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class AlgoExecutionController {

    private final AlgoExecutionService service;
    private final ReplayRunner replayRunner;

    public AlgoExecutionController(AlgoExecutionService service, ReplayRunner replayRunner) {
        this.service = service;
        this.replayRunner = replayRunner;
    }

    @PostMapping
    public ParentOrder submit(@RequestBody ParentOrderRequest request) {
        return service.submit(request);
    }

    @PostMapping("/{parentOrderId}/pause")
    public void pause(@PathVariable String parentOrderId) {
        service.pause(parentOrderId);
    }

    @PostMapping("/{parentOrderId}/resume")
    public void resume(@PathVariable String parentOrderId) {
        service.resume(parentOrderId);
    }

    @PostMapping("/{parentOrderId}/cancel")
    public void cancel(@PathVariable String parentOrderId) {
        service.cancel(parentOrderId);
    }

    @GetMapping("/{parentOrderId}")
    public OrderProgress get(@PathVariable String parentOrderId) {
        return service.query(parentOrderId);
    }

    @GetMapping
    public List<OrderProgress> list() {
        return service.list();
    }

    @GetMapping("/{parentOrderId}/events")
    public List<ExecutionEvent> events(@PathVariable String parentOrderId) {
        return service.events(parentOrderId);
    }

    @PostMapping("/market/ticks")
    public void tick(@RequestBody MarketTick tick) {
        service.onMarketTick(tick);
    }

    @PostMapping("/replay")
    public ReplayResult replay(@RequestBody ReplayRequest request) {
        if (request.getScenario() == null) {
            throw new OrderServiceException("scenario with market ticks is required for replay");
        }
        return replayRunner.run(request, request.getScenario());
    }

    @ExceptionHandler(OrderServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleServiceError(OrderServiceException e) {
        return e.getMessage();
    }
}
