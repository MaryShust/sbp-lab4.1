package com.example.sbp.security;

import com.example.sbp.entity.PrivilegeEntity;
import com.example.sbp.entity.RoleEntity;
import com.example.sbp.exception.FileParseException;
import com.example.sbp.exception.UserAlreadyExistsException;
import com.example.sbp.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class XmlUserDetailsService implements UserDetailsService {

    @Value("${users.xml.path:users.xml}")
    private String xmlPath;

    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public XmlUserDetailsService(PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Document document = loadDocument();
        Element userElement = findUserElement(document, username);

        if (userElement == null) {
            userElement = createNewUser(document, username, null, null);
        }

        return buildUserDetails(userElement);
    }

    public synchronized void createUserWithPhone(String username, String rawPassword, String phoneNumber) {
        Document document = loadDocument();
        if (findUserElement(document, username) != null) {
            log.debug("Пользователь уже существует: {}", username);
            throw new UserAlreadyExistsException("Пользователь уже существует");
        }
        if (phoneNumber != null && !phoneNumber.isBlank() && findUserElementByPhone(document, phoneNumber) != null) {
            log.debug("Номер телефона уже используется: {}", phoneNumber);
            throw new UserAlreadyExistsException("Номер телефона уже используется");
        }
        createNewUser(document, username, rawPassword, phoneNumber);
        saveDocument(document);
        log.debug("Создан новый пользователь: {} с телефоном: {}", username, phoneNumber);
    }

    public synchronized void updateUserRole(String username, String newRoleName) {
        Document document = loadDocument();
        Element userElement = findUserElement(document, username);
        if (userElement == null) {
            throw new UsernameNotFoundException("Пользователь не найден: " + username);
        }
        userElement.setAttribute("roles", newRoleName);
        incrementTokenVersion(userElement);
        saveDocument(document);
        log.debug("Обновили роль для пользователя {}: {}", username, newRoleName);
    }

    public synchronized void updateUserAccountIdByPhone(String phoneNumber, Long accountId) {
        Document document = loadDocument();
        Element userElement = findUserElementByPhone(document, phoneNumber);
        if (userElement == null) {
            log.debug("Не нашли пользователя с телефоном: {}", phoneNumber);
            return;
        }
        userElement.setAttribute("accountId", accountId != null ? accountId.toString() : "");
        incrementTokenVersion(userElement);
        saveDocument(document);
        log.debug("Обновлен accountId для пользователя с номером телефона {}: {}", phoneNumber, accountId);
    }

    public synchronized int incrementTokenVersion(String username) {
        Document document = loadDocument();
        Element userElement = findUserElement(document, username);
        if (userElement == null) {
            throw new UsernameNotFoundException("Пользователь не найден: " + username);
        }
        int newVersion = incrementTokenVersion(userElement);
        saveDocument(document);
        return newVersion;
    }

    public int getTokenVersion(String username) {
        try {
            Document document = loadDocument();
            Element userElement = findUserElement(document, username);
            if (userElement == null) {
                return 0;
            }
            String versionStr = userElement.getAttribute("tokenVersion");
            return !versionStr.isBlank() ? Integer.parseInt(versionStr) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean userExists(String username) {
        try {
            Document document = loadDocument();
            return findUserElement(document, username) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public CustomUserDetails getUser(String username) {
        Document document = loadDocument();
        Element userElement = findUserElement(document, username);
        if (userElement == null) {
            throw new UsernameNotFoundException("Пользователь не найден: " + username);
        }
        return buildUserDetails(userElement);
    }

    private CustomUserDetails buildUserDetails(Element userElement) {
        String username = userElement.getAttribute("username");
        String password = userElement.getAttribute("password");
        String rolesStr = userElement.getAttribute("roles");

        String accountIdStr = userElement.getAttribute("accountId");
        Long accountId = !accountIdStr.isBlank() ? Long.parseLong(accountIdStr) : null;

        String phoneNumber = userElement.getAttribute("phoneNumber");
        if (phoneNumber.isBlank()) {
            phoneNumber = null;
        }

        String versionStr = userElement.getAttribute("tokenVersion");
        int tokenVersion = !versionStr.isBlank() ? Integer.parseInt(versionStr) : 0;

        String role = parseRole(rolesStr);
        Set<Privilege> privileges = collectPrivilegesFromDb(role);

        return new CustomUserDetails(username, password, role, privileges, accountId, phoneNumber, tokenVersion);
    }

    private Document loadDocument() {
        try {
            File file = new File(xmlPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (!file.exists()) {
                Document doc = builder.newDocument();
                doc.appendChild(doc.createElement("users"));
                return doc;
            }
            return builder.parse(file);
        } catch (Exception e) {
            throw new FileParseException("Ошибка загрузки");
        }
    }

    private void saveDocument(Document document)  {
        try {
            File file = new File(xmlPath);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(file));
        } catch (Exception e) {
            throw new FileParseException("Ошибка сохранения");
        }
    }

    private Element findUserElement(Document document, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        NodeList users = document.getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            Element userElement = (Element) users.item(i);
            if (username.equals(userElement.getAttribute("username"))) {
                return userElement;
            }
        }
        return null;
    }

    private Element findUserElementByPhone(Document document, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        NodeList users = document.getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            Element userElement = (Element) users.item(i);
            if (phoneNumber.equals(userElement.getAttribute("phoneNumber"))) {
                return userElement;
            }
        }
        return null;
    }

    private Element createNewUser(Document document, String username, String rawPassword, String phoneNumber) {
        String encodedPassword = rawPassword != null && !rawPassword.isEmpty()
                ? "{bcrypt}" + passwordEncoder.encode(rawPassword)
                : "{bcrypt}" + passwordEncoder.encode(UUID.randomUUID().toString());

        Element userElement = document.createElement("user");
        userElement.setAttribute("username", username);
        userElement.setAttribute("password", encodedPassword);
        userElement.setAttribute("roles", "USER");
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            userElement.setAttribute("phoneNumber", phoneNumber);
        }
        userElement.setAttribute("accountId", "");
        userElement.setAttribute("tokenVersion", "0");

        Element root = document.getDocumentElement();
        root.appendChild(document.createTextNode("\n    "));
        root.appendChild(userElement);

        saveDocument(document);
        return userElement;
    }

    private int incrementTokenVersion(Element userElement) {
        String versionStr = userElement.getAttribute("tokenVersion");
        int currentVersion = !versionStr.isBlank() ? Integer.parseInt(versionStr) : 0;
        int newVersion = currentVersion + 1;
        userElement.setAttribute("tokenVersion", String.valueOf(newVersion));
        return newVersion;
    }

    private String parseRole(String rolesStr) {
        if (rolesStr == null || rolesStr.isBlank()) return "USER";
        return rolesStr;
    }

    private Set<Privilege> collectPrivilegesFromDb(String role) {
        Set<Privilege> privileges = new HashSet<>();
        RoleEntity roleEntity = roleRepository.findByName(role.toUpperCase()).orElse(null);
        if (roleEntity != null) {
            for (PrivilegeEntity privilegeEntity : roleEntity.getPrivileges()) {

                privileges.add(Privilege.valueOf(privilegeEntity.getName()));

            }
        }
        return privileges;
    }
}
