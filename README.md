# SegConf-2026

Projeto de Segurança e Confiabilidade

## Group Members

- Guilherme Dias
- Lana Santos
- Sofia Rodrigues

## Overview & Requirements

This project consist in two applications: a server and a client. The server is responsible for managing the state of the system, including users, houses, sections, and devices. The client is a command-line interface that allows users to interact with the server and perform various operations.

This project was developed in both Ubuntu 24.04 LTS and MacOS, any Java version above Java 8 should suffice to compile and run the project, nevertheless the project was developed using Java 21 LTS.

[Go to Usage](#usage)

## Features

- Users: Each user has a unique username, a password, and can create their own houses. Users can also be added as maintainers to other users' houses.
- Houses: Each house has a unique name, an owner (the user who created it), sections (E - Electros, G-Garden, L-Luzes, M-Multimedia, P-Portas, S-Stores). Each section of the house can have maintainers (users who have access to that section). Houses can be created by users and can have multiple sections.
- Devices: Each device has a unique name, a section (e.g., light, door, multimedia), and a state (on - with timer/off). Devices can be added to sections of houses and can be controlled by maintainers of those sections.
- Persistence: Data is persisted to files, ensuring that the state of the repository is maintained across server restarts.
- Logging: All operations are logged with timestamps, user information, and operation details.
- Concurrency: The server can handle multiple client connections simultaneously using a thread pool.
- Graceful Shutdown: The server can be shut down gracefully, ensuring that all ongoing operations are completed before termination.

### Security Enhancements relative to the second phase

#### Secure Communication (TLS)

- All communication between client and server is performed over **TLS-secured sockets**.
- Authentication is **unilateral** (client verifies server identity).

#### Key and Certificate Management

- The server posses a **keystore** containing their private/public key pair.
- Each user posseses a **keystore** containing their private/public key pair and a **trustore** containing the other user's certificate's with whom the user interacts with.
- The Client application maintains a **truststore** containing Server certificate

#### Secure Storage (Encrypted Persistence)

- Sensitive server data (`users.txt`, `homes.txt`) is encrypted using **PBE (Password-Based Encryption)** derived from a password provided at server startup
- A **persistent salt** is stored (in-code) to ensure deterministic key derivation across executions.

#### Integrity Verification

- Every file `<file>` has a corresponding `<file>.hash` generated using **SHA-256**
- Integrity is verified:
  - On server startup
  - Before file usage
  - On shutdown
- If integrity fails Server outputs `NOK-INTEGRITY` and the execution is immediately terminated

#### End-to-End Encryption (E2EE)

- Device state command (`EC`) are Encrypted **end-to-end**
  - Only readable by users with permission for the corresponding section
- The server:
  - **Stores but cannot decrypt** these values

#### Certificate Distribution

- The server acts as a **certificate distribution service**
- Clients:
  - Request certificates of other users
  - Store them locally in their truststore

#### Remote Attestation

- The server validates the integrity of the client application through:
  - A **remote attestation mechanism**

#### Secure Logging

- Logs:
  - Maintain integrity verification
  - Sensitive data is protected via encryption mechanisms where applicable

## Architecture

![Architecture Diagram](/Diagrama.png)

### File Structure

```plaintext
|clean_build.sh         --> Cleans previous data and rebuilds the project from scratch
|server_run.sh          --> Starts the server (use this after first build)
|client_sofia_run.sh    --> Starts client for user 'sofia'
|client_lana_run.sh     --> Starts client for user 'lana'
|client_gui_run.sh      --> Starts client for user 'gui'
|scripts/               --> Contains build and security scripts
|SpertaClient/          --> Client source code and resources
|      |data/           --> Client data (security, users)
|      |src/            --> Client source code
|SpertaServer/          --> Server source code and resources
|      |data/           --> Server data (houses, user certificates)
|      |security/       --> Server security (keystore, attestation)
|      |src/            --> Server source code
```

#### The Code

```plaintext
|domain/  
|      |core/
|      |...                  --> Classes from the Domain (House, Section, User)
|      |Controller.java      --> Thread class that controls all domain classes
|services/
|      |LoggerService.java   --> Controlls all logging functionality
|      |Repository.java      --> Layer of access to the persistence, works as an in-memmory database
|      |SecurityService.java --> Holds every Security-related logic (permissions, atestation...)
|utils/                      --> Holds utility classes and abstraction layers
|      |enums...
|      |LogEntry.java        --> Represents an action from client and it's log
|      |Persistence.java     --> Abstracts certain creation and manipulation of files
|SpertaClient.java           --> Runs a client and it's CLI
|SpertaServer.java           --> Runs the server and launches Controller threads
```

#### The Data

```plaintext
Client/
|data/                  --> Holds all client files (RH and RT files)
|security/
|   |users/
|       |user1/         --> Holds user's keystore and trustore
|   |trustsotre.client 

Server/
|data/
|    |houses/                --> Holds all Houses's log files
|           |Casa_1/         --> Holds all device's logs from Casa_1
|           |      |E/       --> Holds all device's logs from Casa_1 from Section E and the mantainer's keys
|           |      ||E1.csv  --> Stores the logs from E1 belonging to section E, from House Casa_1
|    |      |      |G/
|    |      |      |L/
|    |      |      |M/
|    |      |      |...
|    |      |Casa_2/
|    |      |      |...
|    |homes.txt              --> Contains the natively serialized in-memmory representation of all registered houses
|    |users.txt              --> Contains the natively serialized in-memmory representation of all registered users
|   |user_certificates       --> Holds the certificates of new users
|security/
|    |attestation.txt        --> After compilation, receives the path of SpertaClient's jar copy
|    |certServer.cer 
|    |keystore.server
   
```

##### How is Data stored?

When exiting, SpertaServer safely closes all threads and persisits the HashMaps inside Repository [see file structure](#file-structure), before storing the data, the server verifies the integrity of the files (using Hashing). These HashMaps are then serialized (using java's native serialization), are encrypted using PBE with the server's passoword and that byte stream is stored in homes.txt and users.txt respectively.
No data is stored while the server is running with the exception of logs. Data is only persisted when the server ends it's execution.

Logs are managed by LoggerService that is called by a Controller thread that has just executed successfully an operation. All logs are appended to their respective Device's and House's log. If there is no log file, LoggerService creates one. Appending to a text file is safe and reasonably easy and quick and this operation is done as quickly as possible. For this reason, these log files might have slightly incongruent timestamps (in the order of the milliseconds) because, to avoid uneccessary bottlenecks, there are no write locks onto these files.

Likewise when doing any write/read operations to the logs, it is verified each log's integrity. But logs are only encrypted using end to end encryption, not PBE.

## Usage

### Building the Project

To build the project from scratch (compiles source code and cleans previous data), run:
```
bash clean_build.sh
```
This will compile the source code, generate the JAR files for both the server and client, clean the previous data from previous runs, and create a new `data` directory.

**Important:** Use `clean_build.sh` only when you make changes to the source files and need to rebuild the project. If no changes were made to the source code, you can skip this step and directly start the server.

### Starting the Server

To start the server (after the first build or if no source changes were made), run:
```
bash server_run.sh
```
This will launch the server application, allowing clients to connect and interact with the system.

### Starting the Client

Before running the client, you may need to update the client script with the server's IP address. Open the desired client script and modify the IP address if the server is running on a different machine.

To start the client, run one of the following scripts:
```
bash client_sofia_run.sh   # Launches client for user 'sofia'
bash client_lana_run.sh    # Launches client for user 'lana'
bash client_gui_run.sh     # Launches client for user 'gui'
```

Alternatively, you can run the client directly with:
```
java -jar SpertaClient.jar <server_ip> <username> <password>
```

**Note:** The default password for all test users is `1234`. Make sure the server is running before starting the client.

## Limitations and Future Work

- The current implementation does not include the delete operations for users, houses, sections, and devices. Future work will involve implementing these operations to allow for more comprehensive management of the system.
- The current design does not allow different users to have the same house name. Future work will involve implementing a more robust naming system to allow for duplicate house names while still maintaining unique identifiers for each house.
- When saving client requests they are saved to `./SpertaClient/data/` . This dir is not protected and can be accessed by anyone that has access to it, not only that, logs are re-written, so if different user's do the same operation on the same house the file that stores that operation is re-written.

**Very Important:**

- The Server only verifies data's integrity when loading up and when shutting down (see how data is stored section), this was a design decision that was made in the first phase of the project, being a core limitation of our design. The same does not happen for logs, which have multiple reads and writes in a server's execution cycle, hence the data is always being verified with each read and write operation.

 We consider that this does not pose any real complexity dephecit, since our modular structure ensures that there is only a single way to store and read files from data.txt and homes.txt. This is relevant, because using this structured approach, it would be easy to add multiple reads/writes to these files.
