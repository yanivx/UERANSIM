#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <unistd.h>
#include <net/if.h>
#include <linux/if_tun.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <arpa/inet.h>
#include <sys/select.h>
#include <sys/time.h>
#include <errno.h>
#include <stdarg.h>

#include "utils.hpp"

static constexpr int BUFFER_SIZE = 65535;
static constexpr int TUN_PORT = 49971;
static constexpr int UERANSIM_PORT = 49972;

bool debug;

static int tun_alloc()
{
    char tun_name[IFNAMSIZ];
    strcpy(tun_name, "hvlgtptun");

    struct ifreq ifr;
    int fd, err;

    if ((fd = open("/dev/net/tun", O_RDWR)) < 0)
    {
        perror("Opening /dev/net/tun");
        return fd;
    }

    memset(&ifr, 0, sizeof(ifr));

    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;

    if (*tun_name)
        strncpy(ifr.ifr_name, tun_name, IFNAMSIZ);

    if ((err = ioctl(fd, TUNSETIFF, (void *)&ifr)) < 0)
    {
        perror("ioctl(TUNSETIFF)");
        close(fd);
        return err;
    }

    strcpy(tun_name, ifr.ifr_name);

    return fd;
}

static int bridge_alloc()
{
    int fd = udp_utils::new_socket("127.0.0.1", TUN_PORT);

    if (fd < 0)
    {
        perror("Creating bridge socket");
        return fd;
    }

    return fd;
}

static int cread(int fd, void *buf, size_t nbytes)
{
    int r = ::read(fd, buf, nbytes);
    if (r < 0)
    {
        perror("Reading");
        exit(EXIT_FAILURE);
    }
    return r;
}

int main(int argc, char *argv[])
{
    int tun_fd = tun_alloc();
    int bridge_fd = bridge_alloc(); // TODO: ensure it is from UERANSIM
    int maxfd = std::max(tun_fd, bridge_fd);

    int8_t buffer[BUFFER_SIZE];

    auto urs = udp_utils::in_address("127.0.0.1", UERANSIM_PORT);

    while (1)
    {
        fd_set fds;

        FD_ZERO(&fds);
        FD_SET(tun_fd, &fds);
        FD_SET(bridge_fd, &fds);

        int ret = select(maxfd + 1, &fds, NULL, NULL, NULL);

        if (ret < 0 && errno == EINTR)
            continue;

        if (ret < 0)
        {
            perror("select()");
            exit(EXIT_FAILURE);
        }

        if (FD_ISSET(tun_fd, &fds))
        {
            int nread = cread(tun_fd, buffer, sizeof(buffer));

            sendto(bridge_fd, buffer, nread, 0, (struct sockaddr *)&urs, sizeof(urs));

            printf("Read %d bytes from TUN\n", nread);
        }

        if (FD_ISSET(bridge_fd, &fds))
        {
            int nread = read(bridge_fd, buffer, sizeof(buffer));

            write(tun_fd, buffer, nread);

            printf("Read %d bytes from Bridge\n", nread);
        }
    }
}