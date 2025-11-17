package engineer.hyper.rag.dtos;

import java.util.List;

public record SearchMemoryResponseDto(List<MemoryEntryDto> memories) {}