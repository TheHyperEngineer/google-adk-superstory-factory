package engineer.hyper.rag.controllers;


import com.google.adk.memory.SearchMemoryResponse;
import com.google.adk.sessions.Session;
import engineer.hyper.rag.dtos.MemoryEntryDto;
import engineer.hyper.rag.dtos.SearchMemoryResponseDto;
import engineer.hyper.rag.services.H2MemoryService;
import io.reactivex.rxjava3.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {
    
    @Autowired
    private H2MemoryService memoryService;
    
    @PostMapping("/add/{sessionId}")
    public Completable addToMemory(@PathVariable String sessionId, @RequestBody Session session) {
        return memoryService.addSessionToMemory(session);
    }

    @GetMapping("/search")
    public Single<SearchMemoryResponseDto> search(@RequestParam String appName, @RequestParam String userId, @RequestParam String query) {
        return memoryService.searchMemory(appName, userId, query)
                .map(response -> {
                    List<MemoryEntryDto> memoryDtos = response.memories().stream()
                            .map(entry -> new MemoryEntryDto(entry.content(), entry.author(), entry.timestamp()))
                            .collect(Collectors.toList());
                    return new SearchMemoryResponseDto(memoryDtos);
                });
    }
}