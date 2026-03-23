# SegConf-2026
Projeto de Segurança e Confiabilidade

## Group Members
- Guilherme Dias
- Lana Santos
- Sofia Rodrigues

## Overview
This project consist in two applications: a server and a client. The server is responsible for managing the state of the system, including users, houses, sections, and devices. The client is a command-line interface that allows users to interact with the server and perform various operations.

## Features
- Users: Each user has a unique username, a password, and can create their own houses. Users can also be added as maintainers to other users' houses.
- Houses: Each house has a unique name, an owner (the user who created it), sections (E - Electros, G-Garden, L-Luzes, M-Multimedia, P-Portas, S-Stores). Each section of the house can have maintainers (users who have access to that section). Houses can be created by users and can have multiple sections.
- Devices: Each device has a unique name, a section (e.g., light, door, multimedia), and a state (on - with timer/off). Devices can be added to sections of houses and can be controlled by maintainers of those sections.
- Persistence: Data is persisted to files, ensuring that the state of the repository is maintained across server restarts.
- Logging: All operations are logged with timestamps, user information, and operation details.
- Concurrency: The server can handle multiple client connections simultaneously using a thread pool.
- Graceful Shutdown: The server can be shut down gracefully, ensuring that all ongoing operations are completed before termination. 

## Usage
### Building the Project and Starting the Server
To build the project, navigate to the root directory and run:
```
bash build.sh
```
It will compile the source code, generating the JAR files for both the server and client. **Also it will also clean the previous data from previous runs** and create a new `data` directory. Finally, it will run the server in the same terminal.

**Important:** This command is to be used for the first time to build the project and start the server. For subsequent runs, you can start the server without rebuilding by using the `server_run.sh` script. 

### Starting the Client
To start the client, navigate to the root directory and run:
```
bash client_run.sh
```
This will launch the client application, allowing you to interact with the server through the command-line interface.

## Limitations and Future Work
- The current implementation does not include the delete operations for users, houses, sections, and devices. Future work will involve implementing these operations to allow for more comprehensive management of the system.
- The current design does not allow different users to have the same house name. Future work will involve implementing a more robust naming system to allow for duplicate house names while still maintaining unique identifiers for each house.
- The current implementation does not include security features such as encryption for user passwords. Future work will involve implementing these security features to enhance the overall security of the system.