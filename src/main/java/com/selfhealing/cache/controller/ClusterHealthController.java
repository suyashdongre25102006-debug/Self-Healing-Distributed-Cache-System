
package com.selfhealing.cache.controller;

import com.selfhealing.cache.cluster.NodeHealth;
import com.selfhealing.cache.service.HeartbeatService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cluster")
public class ClusterHealthController {

    private final HeartbeatService heartbeatService;

    public ClusterHealthController(
            HeartbeatService heartbeatService) {

        this.heartbeatService = heartbeatService;
    }

    @GetMapping("/health")
    public Map<String, NodeHealth> health() {

        return heartbeatService.getNodeHealth();
    }
}
