package com.github.guoyyit.algoexecutionenginejava.api;

import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.service.AlgoExecutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/algo")
public class AlgoController {

    private final AlgoExecutionService service;

    public AlgoController(AlgoExecutionService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public Object submit(@RequestBody OrderRequest request) {
        return service.submit(request);
    }

    @PostMapping("/orders/{id}/pause")
    public Object pause(@PathVariable String id) {
        return service.pause(id);
    }

    @PostMapping("/orders/{id}/resume")
    public Object resume(@PathVariable String id) {
        return service.resume(id);
    }

    @PostMapping("/orders/{id}/cancel")
    public Object cancel(@PathVariable String id) {
        return service.cancel(id);
    }

    @GetMapping("/orders/{id}")
    public Object get(@PathVariable String id) {
        return service.getParent(id);
    }

    @GetMapping("/orders")
    public Object all() {
        return service.allParents();
    }

    @GetMapping("/orders/{id}/plan")
    public Object plan(@PathVariable String id) {
        return service.getPlan(id);
    }

    @GetMapping("/orders/{id}/children")
    public Object children(@PathVariable String id) {
        return service.getChildren(id);
    }

    @GetMapping("/orders/{id}/deviations")
    public Object deviations(@PathVariable String id) {
        return service.getDeviations(id);
    }

    @PostMapping("/ticks/{count}")
    public Object ticks(@PathVariable int count) {
        service.advanceTicks(count);
        return java.util.Map.of("advanced", count);
    }
}
