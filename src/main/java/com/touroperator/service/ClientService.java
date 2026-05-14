package com.touroperator.service;

import com.touroperator.domain.Client;
import com.touroperator.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);
    private final ClientRepository clientRepo;

    @Autowired
    public ClientService(ClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

    public List<Client> findAll() {
        return clientRepo.findAll();
    }

    public Client findById(UUID id) {
        return clientRepo.findById(id).orElse(null);
    }

    public void save(Client client) {
        if (client.getId() == null) client.setId(UUID.randomUUID());
        clientRepo.saveClient(client);
        log.info("Клієнт збережено: {}", client.getName());
    }

    public void delete(UUID id) {
        clientRepo.delete(id);
        log.info("Клієнт видалено: {}", id);
    }
}
