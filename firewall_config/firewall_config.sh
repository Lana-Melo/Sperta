# firewall config
# verificar ordem dos comandos, tem de fazer sentido

#deafut policy bloquear tudo só passa o q tiver nas regras
iptables -P INPUT DROP
iptables -P OUTPUT DROP

#OUTPUT
#(i) ping às máquinas da sua sub-rede local (considerando a máscara 255.255.255.0) e à
#máquina Broker. Contudo, a frequência dos pings deve ser limitada a um máximo de 4
#pings/segundo

# sub-rede local
iptables -A OUTPUT -p icmp -m icmp --icmp-type 8 -d 10.0.0.0/24 -m limit --limit 4/second -j ACCEPT 

# Broker
iptables -A OUTPUT -p icmp -m icmp --icmp-type 8 -d 10.0.2.4 -m limit --limit 4/second -j ACCEPT 

#explicitamente fazer drop antes de chegar à regra do related (sub-rede local)
iptables -A OUTPUT -p icmp -m icmp --icmp-type 8 -d 10.0.0.0/24 -j DROP 

#explicitamente fazer drop antes de chegar à regra do related (Broker)
iptables -A OUTPUT -p icmp -m icmp --icmp-type 8 -d 10.0.2.4 -j DROP 

#iii) O tráfego do dispositivo de loopback (i.e., o localhost; 127.0.0.1) não deve ser filtrado pelas
#regras definidas para a máquina segura onde corre o servidor SpertaServer
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT

#iv) O tráfego relacionado com uma ligação já estabelecida com a máquina segura onde corre o
#servidor SpertaServer também deve ser aceite
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
iptables -A OUTPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

#INPUT
#(i) responde a pings apenas com origem nas máquinas SperAdm, CondAdm e nas
#máquinas da sub-rede em que se encontram as máquinas SperCli1 e SperCli2 (onde
#correrão a aplicação SpertaClient)

iptables -A INPUT -p icmp --icmp-type 8 -s 10.0.0.3 -j ACCEPT # SperAdm
iptables -A INPUT -p icmp --icmp-type 8 -s 10.0.3.2 -j ACCEPT # CondAdm
iptables -A INPUT -p icmp --icmp-type 8 -s 10.0.1.0/24 -j ACCEPT # sub-rede SperCli1 e SperCli2

#(ii) aceita ligações de clientes SpertaClient para o servidor SpertaServer apenas a partir
#de máquinas da sub-rede em que se encontram as máquinas SperCli1 e SperCli2
#(considerando a máscara 255.255.255.0)
#comentario sobre o port

iptables -A INPUT -p tcp -s 10.0.1.0/24 --dport 22345 -m state --state NEW -j ACCEPT

#(iii) aceita ligações ssh apenas da máquina Broker e das máquinas da sub-rede em que
#se encontra o próprio SperServ (considerando a máscara 255.255.255.0)

iptables -A INPUT -p tcp -s 10.0.2.4 --dport 22 -m state --state NEW -j ACCEPT #Broker
iptables -A INPUT -p tcp -s 10.0.0.0/24 --dport 22 -m state --state NEW -j ACCEPT    # sub-rede SperServ

#OUTPUT

#(ii) conexão de ssh à máquina CondServ
iptables -A OUTPUT -p tcp -d 10.0.3.3 --dport 22 -m state --state NEW -j ACCEPT # CondServ



