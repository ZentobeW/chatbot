# ChatBot 

## Installation

1. Clone the repository:
```
git clone https://github.com/ZentobeW/chatbot.git
```

2. Compile the Java files:
```
javac *.java
```

3. Run the server:
```
java mainServer
```

4. Run the client(s):
```
java mainClient
```

## Usage

The ChatBot Server provides a simple chat interface where clients can connect and interact with a chatbot. The server supports the following features:

- Connect and disconnect clients
- Broadcast messages to all connected clients
- Kick specific clients
- Save the server log to a file
- Clear the server log
- Display server status

To use the ChatBot Server, follow these steps:

1. Run the `mainServer` class to start the server.
2. Run the `mainClient` class to start one or more clients.
3. Clients can interact with the chatbot by typing messages in the input field and pressing Enter.
4. The server log will display the messages sent and received, as well as any server commands executed.

## Contributing

If you would like to contribute to the ChatBot Server project, please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and commit them.
4. Push your changes to your forked repository.
5. Submit a pull request to the original repository.

## Testing

To test the ChatBot Server, you can run the `mainClient` class multiple times to simulate multiple clients connecting to the server. You can then interact with the chatbot and observe the server log for any issues or unexpected behavior.

Additionally, you can test the server commands by typing them in the server's input field and verifying the expected behavior.
