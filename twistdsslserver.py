from twisted.internet import ssl, reactor
from twisted.internet.protocol import Factory, Protocol

class SendOK(Protocol):
    def dataReceived(self, data):
        """As soon as any data is received, write it back."""
        print data
        #self.transport.write("200 OK\r\n")
        #self.transport.write("got data: %s" % data)
        self.transport.write("HTTP/1.1 200 OK\r\n\r\n")
        self.transport.write("******** TEST RESPONSE *********")
        self.transport.loseConnection()

if __name__ == '__main__':
    factory = Factory()
    factory.protocol = SendOK
    reactor.listenSSL(443, factory,
                      ssl.DefaultOpenSSLContextFactory(
            'server.key', 'server.crt'))
    reactor.run()