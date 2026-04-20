package com.taskapi.service;

import com.taskapi.dto.UserDTO.*;
import com.taskapi.entity.User;
import com.taskapi.exception.EmailAlreadyExistsException;
import com.taskapi.exception.UserNotFoundException;
import com.taskapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)   // padrão read-only — mais performático
public class UserService {

    private final UserRepository repository;

    // Injeção por construtor — preferível a @Autowired no campo
    // Facilita testes (você consegue passar um mock diretamente)
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<UserResponse> findAll() {
        return repository.findAll()
            .stream()
            .map(UserResponse::from)   // method reference — mais limpo que lambda
            .toList();                 // Java 16+ — mais conciso que collect(toList())
    }

    public UserResponse findById(Long id) {
        return repository.findById(id)
            .map(UserResponse::from)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional   // sobrescreve o readOnly=true da classe
    public UserResponse create(CreateUserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        var user = new User(request.name(), request.email());
        var saved = repository.save(user);

        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        var user = repository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

        // Atualiza apenas os campos que vieram na requisição (null = não alterar)
        if (request.name() != null)  user.setName(request.name());
        if (request.email() != null) {
            if (repository.existsByEmail(request.email())) {
                throw new EmailAlreadyExistsException(request.email());
            }
            user.setEmail(request.email());
        }

        // Não precisa chamar save() — o JPA detecta mudanças na entidade
        // automaticamente dentro de uma transação (@Transactional)
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
