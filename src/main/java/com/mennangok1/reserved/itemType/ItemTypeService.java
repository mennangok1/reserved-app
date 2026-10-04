package com.mennangok1.reserved.itemType;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemTypeService {

    private final ItemTypeRepository itemTypeRepository;

    public ItemTypeService(ItemTypeRepository itemTypeRepository) {
        this.itemTypeRepository = itemTypeRepository;
    }

    public List<ItemTypeResponse> listAll() {
        return itemTypeRepository.findAll().stream()
                .map(ItemTypeResponse::from)
                .toList();
    }

}
