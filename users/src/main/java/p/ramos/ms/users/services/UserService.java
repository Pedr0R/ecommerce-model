package p.ramos.ms.users.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p.ramos.ms.users.UserRepository;
import p.ramos.ms.users.UserModel;
import p.ramos.ms.users.dto.UserRequestDTO;
import p.ramos.ms.users.dto.UserResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        return convertToResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO save(UserRequestDTO request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("E-mail já cadastrado!");
        }
        UserModel user = new UserModel(request.name(), request.email(), request.password());
        UserModel savedUser = userRepository.save(user);
        return convertToResponseDTO(savedUser);
    }

    @Transactional
    public UserResponseDTO update(Long id, UserRequestDTO request) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        
        java.util.Optional<UserModel> existingUserWithEmail = userRepository.findByEmail(request.email());
        if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(id)) {
            throw new RuntimeException("E-mail já cadastrado por outro usuário!");
        }
        
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        
        UserModel updatedUser = userRepository.save(user);
        return convertToResponseDTO(updatedUser);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado com ID: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponseDTO convertToResponseDTO(UserModel user) {
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail());
    }
}
